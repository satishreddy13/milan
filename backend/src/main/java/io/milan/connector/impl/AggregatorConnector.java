package io.milan.connector.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PROCESSOR — aggregates multiple sub-messages back into a single message.
 * Typically placed after a SPLITTER to collect processed records.
 *
 * Completion conditions (use either or both — whichever fires first):
 *   completionSize    — aggregate N messages then emit
 *   completionTimeout — emit after N milliseconds of inactivity
 *
 * Strategies:
 *   collect     — gather all bodies into a JSON array  (default)
 *   concatenate — join bodies as strings with separator
 *   latest      — keep only the latest body
 *
 * Correlation expression groups messages. Use a constant (e.g. "1") to
 * aggregate all split messages together, or ${header.groupId} to group by header.
 */
@Component
public class AggregatorConnector implements ConnectorHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override public String  getType()  { return "AGGREGATOR"; }
    @Override public boolean isSource() { return false; }

    @Override
    public ProcessorDefinition<?> applyAndReturn(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        String correlationExpr  = str(node, "correlationExpression", "1");
        int    completionSize   = intVal(str(node, "completionSize",   "0"));
        long   completionTimeout= longVal(str(node, "completionTimeout","0"));
        String strategy         = str(node, "strategy", "collect");
        String separator        = str(node, "separator", "\n");

        AggregationStrategy aggStrategy = switch (strategy) {
            case "concatenate" -> new ConcatenateStrategy(separator);
            case "latest"      -> new LatestStrategy();
            default            -> new CollectStrategy();   // "collect"
        };

        var agg = route
                .aggregate().simple(correlationExpr, String.class)
                .aggregationStrategy(aggStrategy);

        if (completionSize   > 0) agg.completionSize(completionSize);
        if (completionTimeout > 0) agg.completionTimeout(completionTimeout);
        if (completionSize == 0 && completionTimeout == 0) {
            // "Auto" mode: collect everything from an upstream Splitter.
            // completionFromBatchConsumer fires when the Splitter marks the last
            // sub-exchange complete (CamelBatchComplete=true). The 5 s timeout is a
            // fallback for parallel splitters where the last overall item may take the
            // "otherwise" branch and never reach this aggregator with that flag set.
            agg.completionFromBatchConsumer().completionTimeout(5_000);
        }

        // After aggregation, convert List → JSON string for "collect" strategy
        if ("collect".equals(strategy)) {
            agg.process(exchange -> {
                Object body = exchange.getIn().getBody();
                if (body instanceof List) {
                    exchange.getIn().setBody(JSON.writeValueAsString(body));
                }
            });
        }

        return agg;
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "AGGREGATOR",
                "Aggregator",
                "PROCESSOR",
                "Collects multiple sub-messages into one — place after a Splitter to reassemble",
                List.of(
                        ConfigField.expression("correlationExpression", "Correlation Key", true,  "1"),
                        ConfigField.select    ("strategy",    "Aggregation Strategy", true,  "collect",
                                               "collect", "concatenate", "latest"),
                        ConfigField.text      ("completionSize",    "Complete After N Messages",  false, "0"),
                        ConfigField.text      ("completionTimeout",  "Complete After (ms)",        false, "0"),
                        ConfigField.text      ("separator",          "Separator (concatenate)",    false, "\\n")
                )
        );
    }

    // ── Aggregation strategies ────────────────────────────────────────────

    /** Collect all bodies into a List (serialised to JSON array after aggregation). */
    static class CollectStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange old, Exchange newEx) {
            String body = newEx.getIn().getBody(String.class);
            if (old == null) {
                List<String> list = new ArrayList<>();
                list.add(body);
                newEx.getIn().setBody(list);
                return newEx;
            }
            @SuppressWarnings("unchecked")
            List<String> list = old.getIn().getBody(List.class);
            if (list == null) list = new ArrayList<>();
            list.add(body);
            old.getIn().setBody(list);
            return old;
        }
    }

    /** Concatenate bodies with a delimiter string. */
    static class ConcatenateStrategy implements AggregationStrategy {
        private final String sep;
        ConcatenateStrategy(String sep) { this.sep = sep.replace("\\n", "\n").replace("\\t", "\t"); }

        @Override
        public Exchange aggregate(Exchange old, Exchange newEx) {
            String body = newEx.getIn().getBody(String.class);
            if (old == null) return newEx;
            old.getIn().setBody(old.getIn().getBody(String.class) + sep + body);
            return old;
        }
    }

    /** Keep only the most recent body. */
    static class LatestStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange old, Exchange newEx) { return newEx; }
    }

    private static String str(FlowNode node, String key, String def) {
        if (node.data() == null || node.data().config() == null) return def;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : def;
    }
    private static int  intVal(String s)  { try { return Integer.parseInt(s.trim()); }  catch (Exception e) { return 0;  } }
    private static long longVal(String s) { try { return Long.parseLong(s.trim()); }    catch (Exception e) { return 0L; } }
}
