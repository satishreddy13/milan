package io.milan.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.milan.connector.ConnectorHandler;
import io.milan.connector.ConnectorRegistry;
import io.milan.flow.FlowDefinition;
import io.milan.flow.FlowEdge;
import io.milan.flow.FlowNode;
import io.milan.log.ExecutionLogService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.TryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds Camel routes from a stored FlowDefinition via recursive graph traversal.
 *
 * <h3>Route structure</h3>
 * <ol>
 *   <li><b>Feeder route</b> (optional) — SCHEDULER / FILE_READER → {@code direct:flow-{id}}
 *   <li><b>Main route</b>  — {@code direct:flow-{id}} → recursive DSL from node graph
 * </ol>
 *
 * <h3>Node types handled directly (not via ConnectorHandler)</h3>
 * <ul>
 *   <li>{@code ERROR_HANDLER} — standalone config node; shapes global {@code onException}
 *   <li>{@code CHOICE}        — forks into when/otherwise branches
 *   <li>{@code TRY_CATCH}     — wraps the remaining chain in doTry/doCatch
 * </ul>
 */
public class FlowRouteBuilder extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FlowRouteBuilder.class);
    static final String TRIGGER_SUFFIX = "-trigger";

    /** Standalone node types that are not part of the main processing chain. */
    private static final Set<String> STANDALONE_TYPES = Set.of("ERROR_HANDLER");

    private final String              routeId;
    private final FlowDefinition      definition;
    private final ConnectorRegistry   registry;
    private final ExecutionLogService logService;
    private final UUID                flowId;
    private final TriggerRegistry     triggerRegistry;

    public FlowRouteBuilder(String routeId, FlowDefinition definition,
                            ConnectorRegistry registry, ExecutionLogService logService,
                            UUID flowId, TriggerRegistry triggerRegistry) {
        this.routeId         = routeId;
        this.definition      = definition;
        this.registry        = registry;
        this.logService      = logService;
        this.flowId          = flowId;
        this.triggerRegistry = triggerRegistry;
    }

    // -----------------------------------------------------------------------
    // TryFrame — context for an open doTry/doCatch block
    // -----------------------------------------------------------------------

    private record TryFrame(
            TryDefinition tryDef,
            String        nodeId,
            String        errorAction,    // "log" | "deadLetter" | "rethrow"
            String        deadLetterDir,
            String        logLevel        // "INFO" | "WARN" | "ERROR"
    ) {}

    // -----------------------------------------------------------------------
    // configure()
    // -----------------------------------------------------------------------

    @Override
    public void configure() {
        if (definition.nodes().isEmpty()) {
            throw new IllegalStateException(
                    "Flow " + flowId + " has no nodes — add at least a source node before starting");
        }

        Map<String, FlowNode>       nodeById      = definition.nodes().stream()
                .collect(Collectors.toMap(FlowNode::id, n -> n, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<FlowEdge>> edgesBySource = definition.edges().stream()
                .collect(Collectors.groupingBy(FlowEdge::source));

        // onException must be declared before any from() — configure from ERROR_HANDLER node if present
        Optional<FlowNode> errorHandlerNode = definition.nodes().stream()
                .filter(n -> "ERROR_HANDLER".equals(n.type()))
                .findFirst();
        configureOnException(errorHandlerNode.orElse(null));

        FlowNode sourceNode = findSourceNode(nodeById);
        String   directUri  = "direct:flow-" + flowId;
        configureSource(sourceNode, directUri);

        RouteDefinition mainRoute = from(directUri)
                .routeId(routeId)
                .process(exchange -> logService.log(flowId, null, "INFO",
                        "Flow triggered — exchange: " + exchange.getExchangeId()));

        // Follow ALL outgoing edges from the source node in sequence.
        // A source can have multiple outgoing edges (e.g. a Logger then a Router) — each is
        // chained onto the main route in edge-list order, not just the first.
        List<FlowEdge> firstEdges = edgesBySource.getOrDefault(sourceNode.id(), List.of());
        boolean builtAny = false;
        for (FlowEdge edge : firstEdges) {
            FlowNode next = nodeById.get(edge.target());
            if (next != null && !STANDALONE_TYPES.contains(next.type())) {
                buildRouteSegment(mainRoute, next, nodeById, edgesBySource, new HashSet<>(), null);
                builtAny = true;
            }
        }
        if (!builtAny) {
            mainRoute.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
        }
    }

    // -----------------------------------------------------------------------
    // onException — orchestration-level error handling
    // -----------------------------------------------------------------------

    private void configureOnException(FlowNode cfg) {
        int    maxRetries    = intCfg(cfg, "maxRedeliveries",  0);
        long   retryDelay    = longCfg(cfg, "redeliveryDelay", 1000L);
        String deadLetterDir = strCfg(cfg, "deadLetterDir",   "/tmp/milan-deadletter");
        boolean useDeadLetter = cfg != null && !deadLetterDir.isBlank()
                && !"false".equals(strCfg(cfg, "useDeadLetter", "true"));

        OnExceptionDefinition ex = onException(Exception.class)
                .maximumRedeliveries(maxRetries)
                .redeliveryDelay(retryDelay)
                .process(exchange -> {
                    Exception caught = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String msg = "Flow execution failed: " + (caught != null ? caught.getMessage() : "unknown");
                    Throwable cause = caught != null ? caught.getCause() : null;
                    if (cause != null) msg += " | Cause: " + cause.getMessage();
                    logService.log(flowId, null, "ERROR", msg);
                });

        if (useDeadLetter) {
            String dlDir = deadLetterDir;
            ex.process(exchange -> {
                Exception caught = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String err = caught != null ? caught.getMessage() : "unknown";
                String orig = exchange.getIn().getBody(String.class);
                exchange.getIn().setBody("Error: " + err + "\n---\n" + orig);
            })
            .to("file://" + dlDir + "?fileName=error-${date:now:yyyyMMdd-HHmmssSSS}.txt&autoCreate=true")
            .handled(true);
        } else {
            ex.handled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Recursive route builder
    // -----------------------------------------------------------------------

    private void buildRouteSegment(ProcessorDefinition<?> current,
                                   FlowNode node,
                                   Map<String, FlowNode> nodeById,
                                   Map<String, List<FlowEdge>> edgesBySource,
                                   Set<String> visited,
                                   TryFrame openTry) {

        if (!visited.add(node.id())) {
            log.warn("Cycle or converging path at node {} — skipping", node.id());
            return;
        }

        // Standalone config nodes are not part of the processing chain
        if (STANDALONE_TYPES.contains(node.type())) return;

        // ── CHOICE ─────────────────────────────────────────────────────────
        if ("CHOICE".equals(node.type())) {
            buildChoiceSegment(current, node, nodeById, edgesBySource, visited);
            return;
        }

        // ── TRY_CATCH ───────────────────────────────────────────────────────
        if ("TRY_CATCH".equals(node.type())) {
            TryDefinition tryDef = current.doTry();
            TryFrame frame = new TryFrame(
                    tryDef,
                    node.id(),
                    strCfg(node, "errorAction",   "log"),
                    strCfg(node, "deadLetterDir", "/tmp/milan-deadletter"),
                    strCfg(node, "logLevel",      "ERROR")
            );
            List<FlowEdge> out = edgesBySource.getOrDefault(node.id(), List.of());
            if (out.isEmpty()) {
                closeTryFrame(frame);
            } else {
                FlowNode successor = nodeById.get(out.get(0).target());
                if (successor != null) {
                    buildRouteSegment(tryDef, successor, nodeById, edgesBySource, new HashSet<>(visited), frame);
                } else {
                    closeTryFrame(frame);
                }
            }
            return;
        }

        // ── Linear node ─────────────────────────────────────────────────────
        ConnectorHandler handler = registry.get(node.type());
        ProcessorDefinition<?> next = handler.applyAndReturn(current, node, flowId);
        next.process(exchange -> logService.log(flowId, node.id(), "INFO",
                "Processed by " + node.type()));

        List<FlowEdge> outgoing = edgesBySource.getOrDefault(node.id(), List.of());
        if (outgoing.isEmpty()) {
            if (openTry != null) {
                closeTryFrame(openTry);
            } else {
                next.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
            }
        } else {
            FlowNode successor = nodeById.get(outgoing.get(0).target());
            if (successor != null) {
                buildRouteSegment(next, successor, nodeById, edgesBySource, new HashSet<>(visited), openTry);
            } else if (openTry != null) {
                closeTryFrame(openTry);
            } else {
                next.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
            }
        }
    }

    /** Close a doTry/doCatch block, adding appropriate error handling. */
    private void closeTryFrame(TryFrame frame) {
        String nodeId        = frame.nodeId();
        String errorAction   = frame.errorAction();
        String deadLetterDir = frame.deadLetterDir();
        String lvl           = frame.logLevel();

        var catchDef = frame.tryDef()
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String msg = "Caught in try-catch: " + (ex != null ? ex.getMessage() : "unknown");
                    logService.log(flowId, nodeId, lvl, msg);
                });

        // Note: in doTry/doCatch, exceptions are always caught (like Java try/catch).
        // handled() does not exist on TryDefinition — rethrow requires an explicit throw.
        switch (errorAction) {
            case "deadLetter" -> {
                String dlDir = deadLetterDir;
                catchDef.process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String err  = ex != null ? ex.getMessage() : "unknown";
                    String orig = exchange.getIn().getBody(String.class);
                    exchange.getIn().setBody("Error: " + err + "\n---\n" + orig);
                })
                .to("file://" + dlDir + "?fileName=error-${date:now:yyyyMMdd-HHmmssSSS}.txt&autoCreate=true");
            }
            case "rethrow" -> {
                // Re-raise so the global onException handler picks it up
                catchDef.process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    throw (ex instanceof RuntimeException re) ? re : new RuntimeException(ex);
                });
            }
            // "log" — already logged above; flow continues after the catch block
        }

        frame.tryDef().end();
    }

    // -----------------------------------------------------------------------
    // File archiving — called from FILE_READER onCompletion()
    // -----------------------------------------------------------------------

    /**
     * Moves or deletes the source file after the exchange completes.
     *
     * <p>Invoked via {@code onCompletion()} so it fires regardless of whether the exchange
     * succeeded, was stopped by a Filter node, or failed — preventing the "continuous re-read"
     * loop that occurs when a filter rejects a file and Camel's built-in move is never triggered.
     *
     * @param duplicateAction  {@code rename} (default) — add a timestamp suffix to avoid overwrite;
     *                         {@code overwrite} — replace the existing archive file;
     *                         {@code skip} — delete from source without archiving if the target exists
     */
    private static void archiveSourceFile(Exchange exchange,
                                          String after,
                                          String archiveDir,
                                          String duplicateAction) {
        String absPath = exchange.getIn().getHeader("CamelFileAbsolutePath", String.class);
        if (absPath == null) return;

        File source = new File(absPath);
        if (!source.exists()) return;  // already moved by a previous completion (e.g. restart race)

        switch (after) {
            case "delete" -> {
                if (!source.delete()) {
                    log.warn("FILE_READER: failed to delete processed file: {}", absPath);
                }
            }
            case "none" -> {
                // Intentionally left in the source directory (e.g. for testing).
                // The readLock will re-read it on the next poll — warn so the user knows.
                log.warn("FILE_READER: after=none — file will be re-read on next poll: {}", source.getName());
            }
            default -> {  // "move"
                File destDir = Paths.get(archiveDir).isAbsolute()
                        ? new File(archiveDir)
                        : new File(source.getParentFile(), archiveDir);
                destDir.mkdirs();

                String name     = source.getName();
                String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                String ext      = name.contains(".") ? name.substring(name.lastIndexOf('.'))    : "";
                File   dest     = new File(destDir, name);

                if (dest.exists()) {
                    switch (duplicateAction) {
                        case "overwrite" -> { /* dest already set; Files.move with REPLACE_EXISTING handles it */ }
                        case "skip" -> {
                            // Archive already has this file — remove from source to stop re-read loop
                            if (!source.delete()) {
                                log.warn("FILE_READER: skip duplicate — failed to delete source: {}", absPath);
                            }
                            log.info("FILE_READER: skipped duplicate '{}' (already in archive)", name);
                            return;
                        }
                        default -> {  // "rename" — add timestamp suffix
                            String ts = new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());
                            dest = new File(destDir, baseName + "_" + ts + ext);
                        }
                    }
                }

                try {
                    Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.debug("FILE_READER: archived '{}' → '{}'", source.getName(), dest.getName());
                } catch (IOException e) {
                    log.error("FILE_READER: failed to archive {} → {}: {}", source, dest, e.getMessage());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // CHOICE segment
    // -----------------------------------------------------------------------

    private void buildChoiceSegment(ProcessorDefinition<?> current,
                                    FlowNode choiceNode,
                                    Map<String, FlowNode> nodeById,
                                    Map<String, List<FlowEdge>> edgesBySource,
                                    Set<String> visited) {
        String condition = strCfg(choiceNode, "condition", "${body} != null");

        List<FlowEdge> outgoing      = edgesBySource.getOrDefault(choiceNode.id(), List.of());
        // Collect ALL edges per handle in edge-list order; each is chained sequentially
        // inside its branch so users can connect multiple nodes to the same handle.
        List<FlowEdge> whenEdges      = outgoing.stream().filter(e -> "when".equals(e.sourceHandle())).collect(Collectors.toList());
        List<FlowEdge> otherwiseEdges = outgoing.stream().filter(e -> "otherwise".equals(e.sourceHandle())).collect(Collectors.toList());

        ChoiceDefinition choice = current.choice();

        var when = choice.when().simple(condition);
        when.process(exchange -> logService.log(flowId, choiceNode.id(), "INFO", "Choice → when branch"));
        for (FlowEdge edge : whenEdges) {
            FlowNode whenNext = nodeById.get(edge.target());
            if (whenNext != null) buildRouteSegment(when, whenNext, nodeById, edgesBySource, new HashSet<>(visited), null);
        }
        if (whenEdges.isEmpty()) {
            when.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
        }

        var otherwise = choice.otherwise();
        otherwise.process(exchange -> logService.log(flowId, choiceNode.id(), "INFO", "Choice → otherwise branch"));
        for (FlowEdge edge : otherwiseEdges) {
            FlowNode otherwiseNext = nodeById.get(edge.target());
            if (otherwiseNext != null) buildRouteSegment(otherwise, otherwiseNext, nodeById, edgesBySource, new HashSet<>(visited), null);
        }
        if (otherwiseEdges.isEmpty()) {
            otherwise.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
        }

        choice.end();
    }

    // -----------------------------------------------------------------------
    // Source wiring
    // -----------------------------------------------------------------------

    private void configureSource(FlowNode sourceNode, String directUri) {
        Map<String, Object> cfg = sourceNode.data() != null ? sourceNode.data().config() : Map.of();

        switch (sourceNode.type()) {

            case "HTTP_LISTENER" -> {
                String path   = cfg.getOrDefault("path",   "/webhook").toString();
                String method = cfg.getOrDefault("method", "POST").toString();
                triggerRegistry.register(method, path, flowId);
            }

            case "SCHEDULER" -> {
                String cron = cfg.getOrDefault("cron", "0/30 * * * * ?").toString().trim();
                if (cron.split("\\s+").length == 5) cron = "0 " + cron;
                from("quartz://milan/" + flowId + "?cron=" + cron.replace(" ", "+") + "&stateful=false")
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .setBody().constant("")
                        .to(directUri);
            }

            case "FILE_READER" -> {
                String  directory       = cfg.getOrDefault("directory",       "/tmp/milan-input").toString();
                String  pattern         = cfg.getOrDefault("pattern",         ".*").toString();
                String  after           = cfg.getOrDefault("after",           "move").toString();
                String  archiveDir      = cfg.getOrDefault("archiveDir",      ".done").toString();
                String  duplicateAction = cfg.getOrDefault("duplicateAction", "rename").toString();
                String  charset         = cfg.getOrDefault("charset",         "UTF-8").toString();
                String  parser          = cfg.getOrDefault("parser",          "none").toString();
                boolean hasHeader       = !"false".equals(cfg.getOrDefault("hasHeader", "true").toString());

                // Always noop=true — archiving is handled explicitly in onCompletion() below.
                // This guarantees the file is moved/deleted even when a Filter node stops the
                // exchange early, preventing the "continuous re-read" loop.
                String fileUri = "file://" + directory
                        + "?include=" + pattern
                        + "&delay=2000"
                        + "&readLock=changed"
                        + "&autoCreate=true"
                        + "&charset=" + charset
                        + "&noop=true";

                var feeder = from(fileUri)
                        .routeId(routeId + TRIGGER_SUFFIX)
                        // onCompletion fires for ALL outcomes: success, filter-stop, exception
                        .onCompletion()
                            .process(exchange -> archiveSourceFile(
                                    exchange, after, archiveDir, duplicateAction))
                        .end()
                        .convertBodyTo(String.class);

                if ("csv".equals(parser)) {
                    CsvDataFormat csvFmt = new CsvDataFormat();
                    csvFmt.setUseMaps(hasHeader);
                    feeder.unmarshal(csvFmt)
                          .process(exchange -> exchange.getIn()
                                  .setBody(new ObjectMapper().writeValueAsString(exchange.getIn().getBody())));
                }
                feeder.to(directUri);
            }

            default -> {
                ConnectorHandler handler = registry.get(sourceNode.type());
                from(handler.buildFromUri(sourceNode)).routeId(routeId + TRIGGER_SUFFIX).to(directUri);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Graph helpers
    // -----------------------------------------------------------------------

    private FlowNode findSourceNode(Map<String, FlowNode> nodeById) {
        Set<String> targetIds = definition.edges().stream().map(FlowEdge::target).collect(Collectors.toSet());
        return definition.nodes().stream()
                .filter(n -> !targetIds.contains(n.id()))
                .filter(n -> !STANDALONE_TYPES.contains(n.type()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Flow " + flowId + " has no source node (possible cycle)"));
    }

    // Config helpers — work with FlowNode or null (for defaults)
    private static String strCfg(FlowNode node, String key, String def) {
        if (node == null || node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
    private static int intCfg(FlowNode node, String key, int def) {
        try { return Integer.parseInt(strCfg(node, key, String.valueOf(def))); } catch (NumberFormatException e) { return def; }
    }
    private static long longCfg(FlowNode node, String key, long def) {
        try { return Long.parseLong(strCfg(node, key, String.valueOf(def))); } catch (NumberFormatException e) { return def; }
    }
}
