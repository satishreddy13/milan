package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TRIGGER — listens for incoming HTTP requests on the shared embedded Tomcat.
 * Paths are mounted at /trigger/{configured-path}.
 */
@Component
public class HttpListenerConnector implements ConnectorHandler {

    @Override
    public String getType() { return "HTTP_LISTENER"; }

    @Override
    public boolean isSource() { return true; }

    @Override
    public String buildFromUri(FlowNode node) {
        String path   = str(node, "path", "/webhook");
        String method = str(node, "method", "POST");

        // Normalise: ensure exactly one leading slash, then prefix with /trigger
        String normPath = "/" + path.replaceAll("^/+", "");
        return "platform-http:/trigger" + normPath + "?httpMethodRestrict=" + method;
    }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "HTTP_LISTENER",
                "HTTP Listener",
                "TRIGGER",
                "Starts a flow when an HTTP request arrives at /trigger/{path}",
                List.of(
                        ConfigField.text("path",   "Path",   true, "/webhook"),
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
