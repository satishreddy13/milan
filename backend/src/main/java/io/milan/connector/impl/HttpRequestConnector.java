package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.apache.camel.Exchange;
import org.apache.camel.model.ProcessorDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ACTION — sends an outbound HTTP request to a configured URL.
 */
@Component
public class HttpRequestConnector implements ConnectorHandler {

    @Override
    public String getType() { return "HTTP_REQUEST"; }

    @Override
    public boolean isSource() { return false; }

    @Override
    public void apply(ProcessorDefinition<?> route, FlowNode node, java.util.UUID flowId) {
        String url    = str(node, "url", "http://localhost");
        String method = str(node, "method", "POST");

        // Encode method into a header; the camel-http component reads CamelHttpMethod
        route.setHeader(Exchange.HTTP_METHOD).constant(method)
             .to(url + (url.contains("?") ? "&" : "?") + "bridgeEndpoint=true");
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "HTTP_REQUEST",
                "HTTP Request",
                "ACTION",
                "Sends an HTTP request to an external URL",
                List.of(
                        ConfigField.text("url",    "URL",    true,  "https://example.com/api"),
                        ConfigField.select("method", "Method", true, "POST",
                                "GET", "POST", "PUT", "DELETE", "PATCH")
                )
        );
    }

    private static String str(FlowNode node, String key, String defaultValue) {
        if (node.data() == null || node.data().config() == null) return defaultValue;
        Object v = node.data().config().get(key);
        return v != null ? v.toString() : defaultValue;
    }
}
