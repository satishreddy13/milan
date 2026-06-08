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
 * TRANSFORMER — sets a message header to the result of a Camel Simple expression.
 * Headers set here are accessible later in the flow as ${header.myName}.
 *
 * Examples:
 *   name=filename,  expression=${header.CamelFileName}
 *   name=size,      expression=${header.CamelFileLength}
 *   name=timestamp, expression=${date:now:yyyy-MM-dd HH:mm:ss}
 */
@Component
public class SetHeaderConnector implements ConnectorHandler {

    @Override public String  getType()  { return "SET_HEADER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String name       = str(node, "name",       "myHeader");
        String expression = str(node, "expression", "${body}");
        route.setHeader(name).simple(expression);
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "SET_HEADER",
                "Set Header",
                "TRANSFORMER",
                "Sets a message header using a Simple expression",
                List.of(
                        ConfigField.text("name",             "Header Name", true,  "myHeader"),
                        ConfigField.expression("expression", "Value",       true,  "${header.CamelFileName}")
                )
        );
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
