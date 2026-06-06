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
 * ACTION — writes the current exchange body to a file in the configured directory.
 */
@Component
public class FileWriterConnector implements ConnectorHandler {

    @Override public String  getType()  { return "FILE_WRITER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String directory = str(node, "directory", "/tmp/milan-output");
        String fileName  = str(node, "fileName",  "${date:now:yyyyMMdd-HHmmssSSS}.txt");
        boolean append   = Boolean.parseBoolean(str(node, "append", "false"));

        String uri = "file://" + directory
                + "?fileName=" + fileName
                + "&autoCreate=true"
                + (append ? "&fileExist=Append" : "");

        route.to(uri);
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "FILE_WRITER",
                "File Writer",
                "ACTION",
                "Writes the message body to a file in the specified directory",
                List.of(
                        ConfigField.text  ("directory", "Output Directory", true,  "/tmp/milan-output"),
                        ConfigField.text  ("fileName",  "File Name",        false, "${date:now:yyyyMMdd-HHmmssSSS}.txt"),
                        ConfigField.select("append",    "If File Exists",   true,  "false",
                                          "false", "true")
                )
        );
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
