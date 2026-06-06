package io.milan.flow;

import com.fasterxml.jackson.databind.JsonNode;

/** All fields are optional — only non-null values are applied. */
public record UpdateFlowRequest(
        String   name,
        String   description,
        JsonNode definition
) {}
