package io.milan.flow;

import java.util.Map;

/**
 * Custom data attached to a React Flow node.
 * {@code config} holds connector-specific key/value pairs (e.g. path, method, url).
 */
public record FlowNodeData(
        String label,
        Map<String, Object> config
) {}
