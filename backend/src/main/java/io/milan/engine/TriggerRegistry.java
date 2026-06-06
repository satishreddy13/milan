package io.milan.engine;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps (normalizedPath) → flowId for active HTTP_LISTENER flows.
 * Used by TriggerController to dispatch incoming HTTP requests to the correct Camel direct: route.
 */
@Component
public class TriggerRegistry {

    // key = "METHOD:/normalized/path"  (e.g. "POST:/webhook")
    private final ConcurrentHashMap<String, UUID> entries = new ConcurrentHashMap<>();

    public void register(String method, String path, UUID flowId) {
        entries.put(key(method, path), flowId);
    }

    public void deregister(UUID flowId) {
        entries.values().removeIf(id -> id.equals(flowId));
    }

    public Optional<UUID> resolve(String method, String path) {
        return Optional.ofNullable(entries.get(key(method, path)));
    }

    private static String key(String method, String path) {
        String normPath = "/" + path.replaceAll("^/+", "");
        return method.toUpperCase() + ":" + normPath;
    }
}
