package io.milan.flow;

import java.util.Map;

/**
 * A single node in a flow definition — mirrors the React Flow node model.
 * {@code type} is the connector type (HTTP_LISTENER, HTTP_REQUEST, LOGGER, SET_BODY).
 * {@code data.config} holds the connector-specific configuration.
 */
public record FlowNode(
        String id,
        String type,
        Map<String, Double> position,
        FlowNodeData data
) {}
