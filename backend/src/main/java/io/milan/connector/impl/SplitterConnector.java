package io.milan.connector.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.builder.Builder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.SplitDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * PROCESSOR — splits a message into multiple sub-messages, each processed independently
 * by the subsequent nodes in the chain.
 *
 * Strategies:
 *   line       — split body on newlines (great for text/log files)
 *   csv        — parse CSV and emit one Map per row (first row = headers)
 *   jsonArray  — parse JSON array body and emit one element per item
 *   expression — split using a Camel Simple expression
 *
 * With streaming=true the file is processed without loading it entirely into memory.
 */
@Component
public class SplitterConnector implements ConnectorHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override public String  getType()  { return "SPLITTER"; }
    @Override public boolean isSource() { return false; }

    @Override
    public ProcessorDefinition<?> applyAndReturn(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String  strategy  = str(node, "strategy",  "line");
        boolean streaming = !"false".equals(str(node, "streaming", "true"));
        boolean parallel  = "true".equals(str(node, "parallel",   "false"));

        SplitDefinition split = switch (strategy) {
            case "csv" -> {
                CsvDataFormat csv = new CsvDataFormat();
                csv.setUseMaps(true);   // each row → Map<String,String>
                yield route.unmarshal(csv).split(Builder.body());
            }
            case "jsonArray" -> route
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        List<?> items = JSON.readValue(body, List.class);
                        exchange.getIn().setBody(items);
                    })
                    .split(Builder.body());
            case "expression" -> route.split().simple(str(node, "expression", "${body}"));
            default ->  // "line"
                    route.split(Builder.body().tokenize("\n"));
        };

        if (streaming) split.streaming();
        if (parallel)  split.parallelProcessing();

        return split;
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "SPLITTER",
                "Splitter",
                "PROCESSOR",
                "Splits a message into sub-messages; subsequent nodes process each one",
                List.of(
                        ConfigField.select("strategy",   "Split Strategy",    true,  "line",
                                           "line", "csv", "jsonArray", "expression"),
                        ConfigField.expression("expression", "Expression (if strategy=expression)",
                                               false, "${body}"),
                        ConfigField.select("streaming",  "Streaming (large files)", false, "true",
                                           "true", "false"),
                        ConfigField.select("parallel",   "Parallel Processing", false, "false",
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
