package io.milan.flow;

import java.util.List;

/**
 * The full JSON structure stored in flows.definition.
 * Deserialised from TEXT column; serialised back on save.
 */
public record FlowDefinition(
        List<FlowNode> nodes,
        List<FlowEdge> edges
) {
    public static FlowDefinition empty() {
        return new FlowDefinition(List.of(), List.of());
    }
}
