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
 * PROCESSOR — drops exchanges that do not match a Camel Simple predicate.
 * Subsequent nodes in the chain run only when the condition is true.
 *
 * Examples:
 *   ${header.CamelFileLength} > 0           — skip empty files
 *   ${header.CamelFileName} regex '.*\.csv' — only CSV files
 *   ${body} contains 'ERROR'                — only messages with errors
 */
@Component
public class FilterConnector implements ConnectorHandler {

    @Override public String  getType()  { return "FILTER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public ProcessorDefinition<?> applyAndReturn(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String condition = str(node, "condition", "${body} != null");
        // filter() returns a FilterDefinition; subsequent nodes wire inside the filter block
        return route.filter().simple(condition);
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "FILTER",
                "Filter",
                "PROCESSOR",
                "Only passes messages where the condition is true; drops the rest",
                List.of(
                        ConfigField.expression("condition", "Condition", true,
                                "${header.CamelFileLength} > 0")
                )
        );
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
