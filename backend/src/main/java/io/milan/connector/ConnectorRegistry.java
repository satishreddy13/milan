package io.milan.connector;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectorRegistry {

    private final Map<String, ConnectorHandler> handlers;

    public ConnectorRegistry(List<ConnectorHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(ConnectorHandler::getType, Function.identity()));
    }

    public ConnectorHandler get(String type) {
        ConnectorHandler h = handlers.get(type);
        if (h == null) throw new IllegalArgumentException("Unknown connector type: " + type);
        return h;
    }

    public List<ConnectorDescriptor> allDescriptors() {
        return handlers.values().stream()
                .map(ConnectorHandler::describe)
                .toList();
    }
}
