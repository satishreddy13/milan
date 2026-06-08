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
 * PROCESSOR — sends an asynchronous copy of the current exchange to a destination
 * while the main exchange continues unchanged (fire-and-forget).
 *
 * Useful for:
 *   audit    — save every message to a file without slowing the flow
 *   debug    — log a snapshot of the body at a specific point
 *   fan-out  — replicate to a secondary system
 */
@Component
public class WireTapConnector implements ConnectorHandler {

    @Override public String  getType()  { return "WIRE_TAP"; }
    @Override public boolean isSource() { return false; }

    @Override
    public ProcessorDefinition<?> applyAndReturn(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String destination = str(node, "destination", "log");
        String directory   = str(node, "directory",   "/tmp/milan-wiretap");
        String fileName    = str(node, "fileName",    "${date:now:yyyyMMdd-HHmmssSSS}-tap.txt");

        String uri = switch (destination) {
            case "file" -> "file://" + directory + "?fileName=" + fileName + "&autoCreate=true";
            default     -> "log:io.milan.wiretap?level=INFO&showBody=true&showHeaders=false";
        };

        return route.wireTap(uri);
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "WIRE_TAP",
                "Wire Tap",
                "PROCESSOR",
                "Sends an async copy of the message to a destination; main flow continues unaffected",
                List.of(
                        ConfigField.select("destination", "Tap To",          true,  "log",
                                           "log", "file"),
                        ConfigField.text  ("directory",   "Directory (file)", false, "/tmp/milan-wiretap"),
                        ConfigField.text  ("fileName",    "File Name (file)", false, "${date:now:yyyyMMdd-HHmmssSSS}-tap.txt")
                )
        );
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
