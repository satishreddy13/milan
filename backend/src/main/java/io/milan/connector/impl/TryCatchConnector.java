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
 * PROCESSOR — wraps all subsequent nodes in a doTry/doCatch block.
 *
 * When any node after this one throws an exception the catch block runs and
 * the flow continues or stops depending on errorAction:
 *   log        — log the error and continue (exception swallowed)
 *   deadLetter — write the failed message to a dead-letter directory and stop
 *   rethrow    — propagate to the global onException handler
 *
 * Note: CHOICE nodes inside a TryCatch are not supported.
 * The actual DSL is built by FlowRouteBuilder which holds the TryFrame context.
 */
@Component
public class TryCatchConnector implements ConnectorHandler {

    @Override public String  getType()  { return "TRY_CATCH"; }
    @Override public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        // Intentionally empty — FlowRouteBuilder handles TRY_CATCH directly
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "TRY_CATCH",
                "Try / Catch",
                "PROCESSOR",
                "Wraps subsequent nodes in error handling — catch exceptions and log, reroute, or rethrow",
                List.of(
                        ConfigField.select("errorAction",   "On Error",               true,  "log",
                                           "log", "deadLetter", "rethrow"),
                        ConfigField.text  ("deadLetterDir", "Dead-Letter Directory",   false, "/tmp/milan-deadletter"),
                        ConfigField.select("logLevel",      "Log Level",              false, "ERROR",
                                           "INFO", "WARN", "ERROR")
                )
        );
    }
}
