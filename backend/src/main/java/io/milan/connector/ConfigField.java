package io.milan.connector;

import java.util.List;

/**
 * Schema for a single configuration field shown in the UI form.
 * {@code type} drives the input widget: "string" | "select" | "number" | "boolean" | "textarea"
 */
public record ConfigField(
        String key,
        String label,
        String type,
        boolean required,
        Object defaultValue,
        List<String> options   // non-null only when type == "select"
) {
    public static ConfigField text(String key, String label, boolean required, String defaultValue) {
        return new ConfigField(key, label, "string", required, defaultValue, null);
    }

    public static ConfigField select(String key, String label, boolean required,
                                     String defaultValue, String... options) {
        return new ConfigField(key, label, "select", required, defaultValue, List.of(options));
    }

    public static ConfigField textarea(String key, String label, boolean required, String defaultValue) {
        return new ConfigField(key, label, "textarea", required, defaultValue, null);
    }

    public static ConfigField cron(String key, String label, boolean required, String defaultValue) {
        return new ConfigField(key, label, "cron", required, defaultValue, null);
    }

    /** Camel Simple Language expression — rendered with syntax hints in the UI. */
    public static ConfigField expression(String key, String label, boolean required, String defaultValue) {
        return new ConfigField(key, label, "expression", required, defaultValue, null);
    }
}
