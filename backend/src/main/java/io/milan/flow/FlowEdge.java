package io.milan.flow;

/**
 * A directed edge connecting two nodes — mirrors the React Flow edge model.
 */
public record FlowEdge(
        String id,
        String source,
        String target
) {}
