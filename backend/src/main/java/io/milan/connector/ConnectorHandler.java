package io.milan.connector;

import io.milan.flow.FlowNode;
import org.apache.camel.model.ProcessorDefinition;

import java.util.UUID;

/**
 * SPI for pluggable connectors.
 *
 * <ul>
 *   <li>Source connectors (triggers) implement {@link #buildFromUri} and return {@code true} from {@link #isSource}.</li>
 *   <li>Non-source connectors (processors/actions) implement {@link #apply}.</li>
 * </ul>
 */
public interface ConnectorHandler {

    /** Unique connector type identifier, e.g. {@code "HTTP_LISTENER"}. */
    String getType();

    /** {@code true} if this connector initiates a flow (appears at the start of a route). */
    boolean isSource();

    /**
     * Builds the Camel {@code from()} URI for source connectors.
     * Only called when {@link #isSource()} is {@code true}.
     */
    default String buildFromUri(FlowNode node) {
        throw new UnsupportedOperationException(getType() + " is not a source connector");
    }

    /**
     * Appends Camel DSL steps to {@code route} for non-source connectors.
     * Only called when {@link #isSource()} is {@code false}.
     *
     * @param flowId the owning flow's UUID, available for connectors that write execution logs
     */
    default void apply(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        throw new UnsupportedOperationException(getType() + " is a source connector");
    }

    /**
     * Like {@link #apply} but returns the {@link ProcessorDefinition} that subsequent nodes
     * should chain onto. Most connectors return {@code route} unchanged; structural connectors
     * (Splitter, Filter) return a child definition so downstream nodes wire inside the block.
     */
    default ProcessorDefinition<?> applyAndReturn(ProcessorDefinition<?> route, FlowNode node, UUID flowId) {
        apply(route, node, flowId);
        return route;
    }

    /** Metadata used by the UI to render the palette entry and config form. */
    ConnectorDescriptor describe();
}
