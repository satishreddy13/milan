package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * STANDALONE — configures orchestration-level error handling for the whole flow.
 *
 * Drop this node anywhere on the canvas (it does not connect to other nodes).
 * FlowRouteBuilder detects it by type and uses its config to shape onException:
 *   - retry N times with a configurable delay before giving up
 *   - optionally route failed messages to a dead-letter directory
 *
 * Without this node the flow uses defaults: no retries, no dead letter.
 */
@Component
public class ErrorHandlerConnector implements ConnectorHandler {

    @Override public String  getType()  { return "ERROR_HANDLER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        // Intentionally empty — FlowRouteBuilder handles ERROR_HANDLER out-of-band
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "ERROR_HANDLER",
                "Error Handler",
                "PROCESSOR",
                "Configures global error handling: retries and dead-letter routing (drop anywhere on canvas)",
                List.of(
                        ConfigField.text  ("maxRedeliveries",  "Max Retries",             false, "0"),
                        ConfigField.text  ("redeliveryDelay",  "Retry Delay (ms)",         false, "1000"),
                        ConfigField.select("useDeadLetter",    "Send Failures To",         false, "false",
                                           "false", "true"),
                        ConfigField.text  ("deadLetterDir",    "Dead-Letter Directory",    false, "/tmp/milan-deadletter")
                )
        );
    }
}
