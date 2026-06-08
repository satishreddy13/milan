package io.milan.flow;

/**
 * A directed edge connecting two nodes — mirrors the React Flow edge model.
 * {@code sourceHandle} identifies which output handle the edge originates from.
 * {@code null} means the default (only) output, used for linear chains.
 * CHOICE nodes use {@code "when"} and {@code "otherwise"} handles.
 */
public record FlowEdge(
        String id,
        String source,
        String target,
        String sourceHandle   // nullable — null for plain single-output nodes
) {}
