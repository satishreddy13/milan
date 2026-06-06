package io.milan.engine;

import io.milan.connector.ConnectorHandler;
import io.milan.connector.ConnectorRegistry;
import io.milan.flow.FlowDefinition;
import io.milan.flow.FlowEdge;
import io.milan.flow.FlowNode;
import io.milan.log.ExecutionLogService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Builds one or two Camel routes from a stored FlowDefinition:
 *
 * <ol>
 *   <li><b>Main route</b> — always {@code direct:flow-{flowId}}, carries the processing chain.
 *       This is the entry point for both real triggers and the manual test trigger.
 *   <li><b>Feeder route</b> (optional) — for non-HTTP sources (SCHEDULER, FILE_READER) a second
 *       route connects the real source (quartz / file) to {@code direct:flow-{flowId}}.
 *       HTTP_LISTENER flows use {@link TriggerController} instead of a feeder route.
 * </ol>
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

    @Override
    public void configure() {
        List<FlowNode> chain = buildNodeChain();
        if (chain.isEmpty()) {
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

        FlowNode sourceNode = chain.get(0);
        String   directUri  = "direct:flow-" + flowId;

        // Wire the source to the direct endpoint (may add a feeder route)
        configureSource(sourceNode, directUri);

        // Main processing route — always starts from direct:
        RouteDefinition route = from(directUri)
                .routeId(routeId)
                .process(exchange -> logService.log(flowId, null, "INFO",
                        "Flow triggered — exchange: " + exchange.getExchangeId()));

        for (int i = 1; i < chain.size(); i++) {
            final FlowNode node = chain.get(i);
            ConnectorHandler handler = registry.get(node.type());
            handler.apply(route, node, flowId);
            route.process(exchange -> logService.log(flowId, node.id(), "INFO",
                    "Processed by " + node.type()));
        }

        route.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
    }

    // -----------------------------------------------------------------------
    // Source wiring
    // -----------------------------------------------------------------------

    private void configureSource(FlowNode sourceNode, String directUri) {
        Map<String, Object> cfg = sourceNode.data() != null
                ? sourceNode.data().config() : Map.of();

        switch (sourceNode.type()) {

            case "HTTP_LISTENER" -> {
                // Dispatched by TriggerController via TriggerRegistry — no feeder route needed
                String path   = cfg.getOrDefault("path",   "/webhook").toString();
                String method = cfg.getOrDefault("method", "POST").toString();
                triggerRegistry.register(method, path, flowId);
            }

            case "SCHEDULER" -> {
                String cron = cfg.getOrDefault("cron", "0/30 * * * * ?").toString();
                // Quartz cron URIs use '+' instead of spaces
                String encodedCron = cron.replace(" ", "+");
                from("quartz://milan/" + flowId + "?cron=" + encodedCron + "&stateful=false")
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .setBody().constant("")   // timers carry no payload
                        .to(directUri);
            }

            case "FILE_READER" -> {
                String  directory = cfg.getOrDefault("directory", "/tmp/milan-input").toString();
                String  pattern   = cfg.getOrDefault("pattern",   ".*").toString();
                boolean doDelete  = "delete".equals(cfg.getOrDefault("after", "move").toString());

                String fileUri = "file://" + directory
                        + "?include=" + pattern
                        + "&delay=2000"
                        + "&readLock=changed"
                        + "&autoCreate=true"
                        + (doDelete ? "&delete=true" : "&move=.done");

                from(fileUri)
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .convertBodyTo(String.class)
                        .to(directUri);
            }

            default -> {
                // Generic source connector — delegate to ConnectorHandler.buildFromUri()
                ConnectorHandler handler = registry.get(sourceNode.type());
                from(handler.buildFromUri(sourceNode))
                        .routeId(routeId + TRIGGER_SUFFIX)
                        .to(directUri);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Graph traversal
    // -----------------------------------------------------------------------

    private List<FlowNode> buildNodeChain() {
        if (definition.nodes().isEmpty()) return List.of();

        Map<String, FlowNode> nodeById = new LinkedHashMap<>();
        for (FlowNode n : definition.nodes()) nodeById.put(n.id(), n);

        Set<String> targetIds = new HashSet<>();
        for (FlowEdge e : definition.edges()) targetIds.add(e.target());

        FlowNode source = definition.nodes().stream()
                .filter(n -> !targetIds.contains(n.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Flow " + flowId + " has no source node (possible cycle)"));

        Map<String, String> nextNode = new HashMap<>();
        for (FlowEdge e : definition.edges()) nextNode.put(e.source(), e.target());

        List<FlowNode> chain   = new ArrayList<>();
        Set<String>    visited = new HashSet<>();
        FlowNode       current = source;

        while (current != null) {
            if (!visited.add(current.id())) {
                throw new IllegalStateException("Cycle detected at node " + current.id());
            }
            chain.add(current);
            String nextId = nextNode.get(current.id());
            current = nextId != null ? nodeById.get(nextId) : null;
        }
        return chain;
    }
}
