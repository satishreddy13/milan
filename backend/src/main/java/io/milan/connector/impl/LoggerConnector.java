package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import io.milan.log.ExecutionLogService;
import org.apache.camel.model.ProcessorDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * PROCESSOR — logs the current exchange body (or any Simple expression) to both
 * the application log (SLF4J) and the execution log table so it appears in the UI footer.
 */
@Component
public class LoggerConnector implements ConnectorHandler {

    private static final Logger flowLog = LoggerFactory.getLogger("io.milan.flow");

    private final ExecutionLogService logService;

    public LoggerConnector(ExecutionLogService logService) {
        this.logService = logService;
    }

    @Override public String  getType()  { return "LOGGER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String message = str(node, "message", "${body}");
        String level   = str(node, "level",   "INFO").toUpperCase();
        String nodeId  = node.id();

        route.process(exchange -> {
            String resolved = exchange.getContext()
                    .resolveLanguage("simple")
                    .createExpression(message)
                    .evaluate(exchange, String.class);

            // Write to application log at the configured level
            switch (level) {
                case "TRACE" -> flowLog.trace(resolved);
                case "DEBUG" -> flowLog.debug(resolved);
                case "WARN"  -> flowLog.warn(resolved);
                case "ERROR" -> flowLog.error(resolved);
                default      -> flowLog.info(resolved);
            }

            // Also write to the execution log so it appears in the UI footer
            logService.log(flowId, nodeId, level, resolved);
        });
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "LOGGER",
                "Logger",
                "PROCESSOR",
                "Logs the exchange body or a Simple expression to the application log and execution log",
                List.of(
                        ConfigField.expression("message", "Message", true, "${body}"),
                        ConfigField.select("level", "Level", true, "INFO",
                                "TRACE", "DEBUG", "INFO", "WARN", "ERROR")
                )
        );
    }

    private static String str(FlowNode node, String key, String defaultValue) {
        if (node.data() == null || node.data().config() == null) return defaultValue;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : defaultValue;
    }
}
