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
import java.util.UUID;

/**
 * TRANSFORMER — parses a CSV string body into a JSON array.
 * When hasHeader=true (default), each row becomes a JSON object keyed by the header row.
 * When hasHeader=false, each row becomes a JSON array of strings.
 */
@Component
public class CsvParserConnector implements ConnectorHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override public String  getType()  { return "CSV_PARSER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String  sep       = str(node, "separator",  ",");
        boolean hasHeader = !"false".equals(str(node, "hasHeader", "true"));
        String  charset   = str(node, "charset",    "UTF-8");

        CsvDataFormat csv = new CsvDataFormat();
        csv.setUseMaps(hasHeader);
        if (!",".equals(sep)) csv.setDelimiter(sep.charAt(0));

        route.unmarshal(csv)
             .process(exchange -> {
                 Object parsed = exchange.getIn().getBody();
                 exchange.getIn().setBody(JSON.writeValueAsString(parsed));
                 exchange.getIn().setHeader("Content-Type", "application/json");
             });
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "CSV_PARSER",
                "CSV Parser",
                "TRANSFORMER",
                "Parses a CSV string body into a JSON array of objects",
                List.of(
                        ConfigField.select("hasHeader",  "Has Header Row", false, "true",  "true", "false"),
                        ConfigField.select("separator",  "Separator",      false, ",",     ",", ";", "|", "\t"),
                        ConfigField.select("charset",    "Charset",        false, "UTF-8", "UTF-8", "UTF-16", "ISO-8859-1")
                )
        );
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
}
