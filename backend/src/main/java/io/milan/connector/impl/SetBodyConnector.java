package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TRANSFORMER — replaces the exchange body with a Camel Simple expression.
 * Use ${body} to pass through, or a static string / expression like ${header.myHeader}.
 */
@Component
public class SetBodyConnector implements ConnectorHandler {

    @Override
    public String getType() { return "SET_BODY"; }

    @Override
    public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, java.util.UUID flowId) {
        String expression = str(node, "expression", "${body}");
        route.setBody().simple(expression);
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "SET_BODY",
                "Set Body",
                "TRANSFORMER",
                "Sets the message body using a Camel Simple expression",
                List.of(
                        ConfigField.textarea("expression", "Expression", true, "${body}")
                )
        );
    }

    private static String str(FlowNode node, String key, String defaultValue) {
        if (node.data() == null || node.data().config() == null) return defaultValue;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : defaultValue;
    }
}
