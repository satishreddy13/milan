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
 * Dynamically builds a Camel route from a stored FlowDefinition.
 *
 * <p>HTTP_LISTENER source nodes use {@code direct:flow-{flowId}} so there are no dynamic
 * Spring MVC mappings — a single {@link TriggerController} handles all /trigger/** traffic
 * and dispatches here via {@link TriggerRegistry}.
 *
 * <p>Phase 1 constraint: flows are linear chains (one source node, no branching).
 */
public class FlowRouteBuilder extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FlowRouteBuilder.class);

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
        String   fromUri    = resolveSourceUri(sourceNode);

        RouteDefinition route = from(fromUri)
                .routeId(routeId)
                .process(exchange -> logService.log(flowId, null, "INFO",
                        "Flow triggered — exchange: " + exchange.getExchangeId()));

        // Apply remaining nodes in order
        for (int i = 1; i < chain.size(); i++) {
            final FlowNode node = chain.get(i);
            ConnectorHandler handler = registry.get(node.type());
            handler.apply(route, node, flowId);
            route.process(exchange -> logService.log(flowId, node.id(), "INFO",
                    "Processed by " + node.type()));
        }

        route.process(exchange -> logService.log(flowId, null, "INFO", "Flow execution completed"));
    }

    /**
     * For HTTP_LISTENER: registers path+method in TriggerRegistry and returns a
     * {@code direct:flow-{flowId}} URI so no dynamic HTTP mapping is needed.
     * For all other source types: delegates to the ConnectorHandler.
     */
    private String resolveSourceUri(FlowNode sourceNode) {
        if ("HTTP_LISTENER".equals(sourceNode.type())) {
            Map<String, Object> cfg = sourceNode.data() != null ? sourceNode.data().config() : Map.of();
            String path   = cfg.getOrDefault("path",   "/webhook").toString();
            String method = cfg.getOrDefault("method", "POST").toString();
            triggerRegistry.register(method, path, flowId);
            return "direct:flow-" + flowId;
        }
        ConnectorHandler handler = registry.get(sourceNode.type());
        return handler.buildFromUri(sourceNode);
    }

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
