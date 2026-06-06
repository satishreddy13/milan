package io.milan.connector;

import java.util.List;

/**
 * Metadata about a connector — returned to the UI so it can render
 * the palette entry and the dynamic config form.
 */
public record ConnectorDescriptor(
        String type,
        String label,
        String category,      // TRIGGER | PROCESSOR | TRANSFORMER | ACTION
        String description,
        List<ConfigField> configFields
) {}
