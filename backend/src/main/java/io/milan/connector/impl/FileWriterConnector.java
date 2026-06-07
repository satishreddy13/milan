package io.milan.connector.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ACTION — writes the current exchange body to a file in the configured directory.
 */
@Component
public class FileWriterConnector implements ConnectorHandler {

    @Override public String  getType()  { return "FILE_WRITER"; }
    @Override public boolean isSource() { return false; }

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String  directory = str(node, "directory", "/tmp/milan-output");
        String  fileName  = str(node, "fileName",  "${date:now:yyyyMMdd-HHmmssSSS}.txt");
        boolean append    = Boolean.parseBoolean(str(node, "append",   "false"));
        String  charset   = str(node, "charset",   "UTF-8");
        String  format    = str(node, "format",    "text");

        // If format=csv and body is a JSON array, marshal it back to CSV first
        if ("csv".equals(format)) {
            CsvDataFormat csv = new CsvDataFormat();
            csv.setHeaderDisabled(false);
            route.marshal(csv);
        }

        String uri = "file://" + directory
                + "?fileName="    + fileName
                + "&autoCreate=true"
                + "&charset="     + charset
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
                        ConfigField.select("format",    "Format",           false, "text",
                                          "text", "csv"),
                        ConfigField.select("append",    "If File Exists",   false, "false",
                                          "false", "true"),
                        ConfigField.select("charset",   "Charset",          false, "UTF-8",
                                          "UTF-8", "UTF-16", "ISO-8859-1")
                )
        );
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
