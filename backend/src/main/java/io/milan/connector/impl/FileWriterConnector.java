package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String  directory = str(node, "directory", "/tmp/milan-output");
        String  fileName  = str(node, "fileName",  "${date:now:yyyyMMdd-HHmmssSSS}.txt");
        boolean append    = Boolean.parseBoolean(str(node, "append",   "false"));
        String  charset   = str(node, "charset",   "UTF-8");
        String  format    = str(node, "format",    "text");

        if ("csv".equals(format)) {
            // Normalise the body to List<List<?>> — the form Camel's CsvDataFormat expects
            // for marshalling. Handles three incoming shapes:
            //   String             → already serialised; skip conversion
            //   Map<?,?>           → single row from a Splitter; wrap in a list
            //   Iterable<Map<?,?>> → full table (List<Map>) from a CSV_PARSER node
            //   Iterable<Iterable> → already row-oriented; pass through
            route.process(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof String) {
                    return;  // already text — write as-is, skip marshal step
                }

                List<List<?>> rows = new ArrayList<>();
                if (body instanceof Map<?, ?> map) {
                    rows.add(new ArrayList<>(map.values()));
                } else if (body instanceof Iterable<?> iterable) {
                    for (Object item : iterable) {
                        if (item instanceof Map<?, ?> m) {
                            rows.add(new ArrayList<>(m.values()));
                        } else if (item instanceof Iterable<?> row) {
                            List<Object> cols = new ArrayList<>();
                            row.forEach(cols::add);
                            rows.add(cols);
                        }
                    }
                }

                if (rows.isEmpty()) {
                    exchange.getIn().setBody("");
                    return;
                }
                exchange.getIn().setBody(rows);
            });

            // Camel's CsvDataFormat marshaller is initialised lazily during start().
            // When routes are added to a running CamelContext the lifecycle may not call
            // start() automatically, so we do it here explicitly before wiring the format
            // into the route to guarantee the internal CsvMarshaller is non-null.
            CsvDataFormat csvFmt = new CsvDataFormat();
            try { csvFmt.start(); } catch (Exception e) {
                throw new RuntimeException("CsvDataFormat failed to start", e);
            }
            route.marshal(csvFmt);
        }

        String uri = "file://" + directory
                + "?fileName="    + fileName
                + "&autoCreate=true"
                + "&charset="     + charset
                + (append ? "&fileExist=Append" : "&fileExist=Override");

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
