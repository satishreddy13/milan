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
 * PROCESSOR — Content-Based Router.
 *
 * Routes messages down one of two branches based on a Simple predicate:
 *   when       (top handle)    — condition is true
 *   otherwise  (bottom handle) — condition is false
 *
 * The actual Camel choice().when().otherwise() DSL is built in FlowRouteBuilder
 * which has access to the full graph. This connector only provides the descriptor
 * and acts as a sentinel type the builder recognises by name.
 */
@Component
public class ChoiceConnector implements ConnectorHandler {

    @Override public String  getType()  { return "CHOICE"; }
    @Override public boolean isSource() { return false; }

    /** Not used — FlowRouteBuilder handles CHOICE nodes directly. */
    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        throw new UnsupportedOperationException(
                "CHOICE is handled by FlowRouteBuilder, not via apply()");
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "CHOICE",
                "Content-Based Router",
                "PROCESSOR",
                "Routes messages to 'when' branch if condition is true, otherwise to 'otherwise' branch",
                List.of(
                        ConfigField.expression("condition", "When Condition", true,
                                "${header.CamelFileName} regex '.*\\.csv'")
                )
        );
    }
}
