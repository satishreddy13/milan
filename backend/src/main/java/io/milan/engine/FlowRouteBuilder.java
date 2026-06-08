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
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds Camel routes from a stored FlowDefinition using recursive graph traversal.
 *
 * <p>Routes:
 * <ol>
 *   <li><b>Feeder route</b> (optional) — SCHEDULER / FILE_READER source → {@code direct:flow-{id}}
 *   <li><b>Main route</b> — {@code direct:flow-{id}} → recursive DSL built from the node graph
 * </ol>
 *
 * <p>Graph model:
 * <ul>
 *   <li>Linear nodes have a single outgoing edge (no {@code sourceHandle}).
 *   <li>CHOICE nodes have two outgoing edges: {@code sourceHandle="when"} and {@code sourceHandle="otherwise"}.
 *       Each branch is built recursively, so branches are independent and can have different lengths.
 * </ul>
 */
public class FlowRouteBuilder extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FlowRouteBuilder.class);

    static final String TRIGGER_SUFFIX = "-trigger";

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
    // configure()
    // -----------------------------------------------------------------------

    @Override
    public void configure() {
        if (definition.nodes().isEmpty()) {
            throw new IllegalStateException(
                    "Flow " + flowId + " has no nodes — add at least a source node before starting");
        }

        // onException must be declared before any from() in Camel RouteBuilder
        onException(Exception.class)
                .process(exchange -> {
                    Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    logService.log(flowId, null, "ERROR",
                            "Flow execution failed: " + (ex != null ? ex.getMessage() : "unknown"));
                })
                .handled(true);

        // Pre-build lookup maps used throughout traversal
        Map<String, FlowNode>       nodeById      = definition.nodes().stream()
                .collect(Collectors.toMap(FlowNode::id, n -> n, (a, b) -> a, LinkedHashMap::new));
        Map<String, List<FlowEdge>> edgesBySource = definition.edges().stream()
                .collect(Collectors.groupingBy(FlowEdge::source));

        FlowNode sourceNode = findSourceNode(nodeById);
        String   directUri  = "direct:flow-" + flowId;

        configureSource(sourceNode, directUri, edgesBySource);

        RouteDefinition mainRoute = from(directUri)
                .routeId(routeId)
                .process(exchange -> logService.log(flowId, null, "INFO",
                        "Flow triggered — exchange: " + exchange.getExchangeId()));

        // Walk edges from the source node into the rest of the graph
        List<FlowEdge> firstEdges = edgesBySource.getOrDefault(sourceNode.id(), List.of());
        if (!firstEdges.isEmpty()) {
            FlowNode first = nodeById.get(firstEdges.get(0).target());
            if (first != null) {
                buildRouteSegment(mainRoute, first, nodeById, edgesBySource, new HashSet<>());
                return; // completion log emitted inside the recursive walk
            }
        }

        // Single-node flow (source only) — just mark it done
        mainRoute.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
    }

    // -----------------------------------------------------------------------
    // Recursive route builder
    // -----------------------------------------------------------------------

    /**
     * Appends DSL for {@code node} onto {@code current}, then recurses to successor nodes.
     * CHOICE nodes fork into two branches; all other nodes are linear.
     */
    private void buildRouteSegment(ProcessorDefinition<?> current,
                                   FlowNode node,
                                   Map<String, FlowNode> nodeById,
                                   Map<String, List<FlowEdge>> edgesBySource,
                                   Set<String> visited) {
        if (!visited.add(node.id())) {
            log.warn("Cycle or converging path detected at node {} — skipping", node.id());
            return;
        }

        if ("CHOICE".equals(node.type())) {
            buildChoiceSegment(current, node, nodeById, edgesBySource, visited);
            return; // branches terminate independently
        }

        // ── Linear node ───────────────────────────────────────────────────
        ConnectorHandler handler = registry.get(node.type());
        ProcessorDefinition<?> next = handler.applyAndReturn(current, node, flowId);
        next.process(exchange -> logService.log(flowId, node.id(), "INFO",
                "Processed by " + node.type()));

        List<FlowEdge> outgoing = edgesBySource.getOrDefault(node.id(), List.of());
        if (outgoing.isEmpty()) {
            next.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
        } else {
            // Follow the single outgoing edge (or the first one if somehow there are several)
            FlowNode successor = nodeById.get(outgoing.get(0).target());
            if (successor != null) {
                buildRouteSegment(next, successor, nodeById, edgesBySource, new HashSet<>(visited));
            } else {
                next.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
            }
        }
    }

    /**
     * Builds a Camel {@code choice().when(...).otherwise()} block.
     * Edges leaving the CHOICE node must carry {@code sourceHandle="when"} or
     * {@code sourceHandle="otherwise"}.
     */
    private void buildChoiceSegment(ProcessorDefinition<?> current,
                                    FlowNode choiceNode,
                                    Map<String, FlowNode> nodeById,
                                    Map<String, List<FlowEdge>> edgesBySource,
                                    Set<String> visited) {
        String condition = cfgStr(choiceNode, "condition", "${body} != null");

        List<FlowEdge> outgoing = edgesBySource.getOrDefault(choiceNode.id(), List.of());
        FlowEdge whenEdge      = outgoing.stream()
                .filter(e -> "when".equals(e.sourceHandle())).findFirst().orElse(null);
        FlowEdge otherwiseEdge = outgoing.stream()
                .filter(e -> "otherwise".equals(e.sourceHandle())).findFirst().orElse(null);

        logService.log(flowId, choiceNode.id(), "INFO",
                "Evaluating choice: " + condition);

        ChoiceDefinition choice = current.choice();

        // ── when branch ───────────────────────────────────────────────────
        var when = choice.when().simple(condition);
        when.process(exchange -> logService.log(flowId, choiceNode.id(), "INFO",
                "Choice → when branch"));
        if (whenEdge != null) {
            FlowNode whenNext = nodeById.get(whenEdge.target());
            if (whenNext != null) {
                buildRouteSegment(when, whenNext, nodeById, edgesBySource, new HashSet<>(visited));
            }
        }

        // ── otherwise branch ──────────────────────────────────────────────
        var otherwise = choice.otherwise();
        otherwise.process(exchange -> logService.log(flowId, choiceNode.id(), "INFO",
                "Choice → otherwise branch"));
        if (otherwiseEdge != null) {
            FlowNode otherwiseNext = nodeById.get(otherwiseEdge.target());
            if (otherwiseNext != null) {
                buildRouteSegment(otherwise, otherwiseNext, nodeById, edgesBySource, new HashSet<>(visited));
            }
        }

        choice.end();
    }

    // -----------------------------------------------------------------------
    // Source wiring
    // -----------------------------------------------------------------------

    private void configureSource(FlowNode sourceNode, String directUri,
                                 Map<String, List<FlowEdge>> edgesBySource) {
        Map<String, Object> cfg = sourceNode.data() != null
                ? sourceNode.data().config() : Map.of();

        switch (sourceNode.type()) {

            case "HTTP_LISTENER" -> {
                String path   = cfg.getOrDefault("path",   "/webhook").toString();
                String method = cfg.getOrDefault("method", "POST").toString();
                triggerRegistry.register(method, path, flowId);
            }

            case "SCHEDULER" -> {
                String cron = cfg.getOrDefault("cron", "0/30 * * * * ?").toString().trim();
                if (cron.split("\\s+").length == 5) cron = "0 " + cron;
                String encodedCron = cron.replace(" ", "+");
                from("quartz://milan/" + flowId + "?cron=" + encodedCron + "&stateful=false")
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .setBody().constant("")
                        .to(directUri);
            }

            case "FILE_READER" -> {
                String  directory  = cfg.getOrDefault("directory",  "/tmp/milan-input").toString();
                String  pattern    = cfg.getOrDefault("pattern",    ".*").toString();
                String  after      = cfg.getOrDefault("after",      "move").toString();
                String  archiveDir = cfg.getOrDefault("archiveDir", ".done").toString();
                String  charset    = cfg.getOrDefault("charset",    "UTF-8").toString();
                String  parser     = cfg.getOrDefault("parser",     "none").toString();
                boolean hasHeader  = !"false".equals(cfg.getOrDefault("hasHeader", "true").toString());

                String fileUri = "file://" + directory
                        + "?include="    + pattern
                        + "&delay=2000"
                        + "&readLock=changed"
                        + "&autoCreate=true"
                        + "&charset="    + charset
                        + switch (after) {
                            case "delete" -> "&delete=true";
                            case "none"   -> "&noop=true";
                            default       -> "&move=" + archiveDir;
                          };

                var feeder = from(fileUri)
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .convertBodyTo(String.class);

                if ("csv".equals(parser)) {
                    CsvDataFormat csvFmt = new CsvDataFormat();
                    csvFmt.setUseMaps(hasHeader);
                    feeder.unmarshal(csvFmt)
                          .process(exchange -> {
                              Object body = exchange.getIn().getBody();
                              exchange.getIn().setBody(new ObjectMapper().writeValueAsString(body));
                          });
                }

                feeder.to(directUri);
            }

            default -> {
                ConnectorHandler handler = registry.get(sourceNode.type());
                from(handler.buildFromUri(sourceNode))
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .to(directUri);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Graph helpers
    // -----------------------------------------------------------------------

    private FlowNode findSourceNode(Map<String, FlowNode> nodeById) {
        Set<String> targetIds = definition.edges().stream()
                .map(FlowEdge::target).collect(Collectors.toSet());
        return definition.nodes().stream()
                .filter(n -> !targetIds.contains(n.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Flow " + flowId + " has no source node (possible cycle)"));
    }

    private static String cfgStr(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
