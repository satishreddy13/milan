package io.milan.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.milan.connector.ConnectorRegistry;
import io.milan.flow.Flow;
import io.milan.flow.FlowDefinition;
import io.milan.flow.FlowRepository;
import io.milan.flow.FlowStatus;
import io.milan.log.ExecutionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.CamelContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FlowEngine {

    private static final Logger log = LoggerFactory.getLogger(FlowEngine.class);

    private final CamelContext        camelContext;
    private final ConnectorRegistry   connectorRegistry;
    private final ObjectMapper        objectMapper;
    private final FlowRepository      flowRepository;
    private final ExecutionLogService logService;
    private final TriggerRegistry     triggerRegistry;

    private final Map<UUID, String>        activeRoutes = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> routeLocks   = new ConcurrentHashMap<>();

    public FlowEngine(CamelContext camelContext, ConnectorRegistry connectorRegistry,
                      ObjectMapper objectMapper, FlowRepository flowRepository,
                      ExecutionLogService logService, TriggerRegistry triggerRegistry) {
        this.camelContext      = camelContext;
        this.connectorRegistry = connectorRegistry;
        this.objectMapper      = objectMapper;
        this.flowRepository    = flowRepository;
        this.logService        = logService;
        this.triggerRegistry   = triggerRegistry;
    }

    // -----------------------------------------------------------------------
    // Startup recovery
    // -----------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent.class)
    public void recoverActiveFlows() {
        flowRepository.findByStatus(FlowStatus.ACTIVE).forEach(flow -> {
            try {
                log.info("Recovering active flow: {} ({})", flow.getName(), flow.getId());
                doStart(flow);
            } catch (Exception e) {
                log.error("Failed to recover flow {}: {}", flow.getId(), e.getMessage());
                flow.setStatus(FlowStatus.ERROR);
                flowRepository.save(flow);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Public lifecycle
    // -----------------------------------------------------------------------

    @Transactional
    public void startFlow(UUID flowId) throws Exception {
        Flow flow = flowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
        doStart(flow);
        flow.setStatus(FlowStatus.ACTIVE);
        flowRepository.save(flow);
    }

    @Transactional
    public void stopFlow(UUID flowId) throws Exception {
        ReentrantLock lock = routeLocks.computeIfAbsent(flowId, k -> new ReentrantLock());
        lock.lock();
        try {
            String routeId = activeRoutes.remove(flowId);
            if (routeId != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
                log.info("Stopped route {} for flow {}", routeId, flowId);
            }
            triggerRegistry.deregister(flowId);
            flowRepository.findById(flowId).ifPresent(flow -> {
                flow.setStatus(FlowStatus.INACTIVE);
                flowRepository.save(flow);
            });
        } finally {
            lock.unlock();
        }
    }

    public boolean isActive(UUID flowId) {
        return activeRoutes.containsKey(flowId);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void doStart(Flow flow) throws Exception {
        ReentrantLock lock = routeLocks.computeIfAbsent(flow.getId(), k -> new ReentrantLock());
        lock.lock();
        try {
            String existing = activeRoutes.get(flow.getId());
            if (existing != null) {
                camelContext.getRouteController().stopRoute(existing);
                camelContext.removeRoute(existing);
            }

            FlowDefinition definition = objectMapper.readValue(flow.getDefinition(), FlowDefinition.class);
            String routeId = "milan-flow-" + flow.getId();

            FlowRouteBuilder builder = new FlowRouteBuilder(
                    routeId, definition, connectorRegistry, logService, flow.getId(), triggerRegistry);
            camelContext.addRoutes(builder);
            camelContext.getRouteController().startRoute(routeId);

            activeRoutes.put(flow.getId(), routeId);
            log.info("Started route {} for flow '{}'", routeId, flow.getName());
        } finally {
            lock.unlock();
        }
    }
}
