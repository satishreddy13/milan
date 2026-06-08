package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
            // If the body is already a String (raw CSV read from file, JSON text, etc.)
            // write it as-is — no re-serialisation needed.
            //
            // For structured bodies (List<Map<String,Object>> from a CSV_PARSER node)
            // serialize to CSV using Apache Commons CSV directly. This avoids the Camel
            // CsvDataFormat lifecycle issue where the internal CsvMarshaller is null when
            // the DataFormat is instantiated outside of Camel's route build lifecycle.
            final String capturedCharset = charset;
            route.process(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof String) {
                    return;  // already text — write as-is
                }
                if (!(body instanceof Iterable<?> iterable)) {
                    return;  // unknown type — let file component write toString()
                }
                List<Object> rows = new ArrayList<>();
                iterable.forEach(rows::add);
                if (rows.isEmpty()) {
                    exchange.getIn().setBody("");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                if (rows.get(0) instanceof Map<?, ?> firstRow) {
                    // List<Map<String,Object>> — write with header row
                    String[] headers = firstRow.keySet().stream()
                            .map(Object::toString).toArray(String[]::new);
                    try (CSVPrinter printer = new CSVPrinter(sb,
                            CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
                        for (Object row : rows) {
                            if (row instanceof Map<?, ?> map) {
                                List<String> values = java.util.Arrays.stream(headers)
                                        .map(h -> {
                                            Object v = map.get(h);
                                            return v != null ? v.toString() : "";
                                        })
                                        .toList();
                                printer.printRecord(values);
                            }
                        }
                    }
                } else {
                    // List<List<?>> — write rows as-is
                    try (CSVPrinter printer = new CSVPrinter(sb, CSVFormat.DEFAULT)) {
                        for (Object row : rows) {
                            if (row instanceof Iterable<?> cols) {
                                printer.printRecord(cols);
                            }
                        }
                    }
                }
                exchange.getIn().setBody(sb.toString());
            });
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
