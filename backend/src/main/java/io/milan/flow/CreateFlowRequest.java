package io.milan.flow;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record CreateFlowRequest(
        @NotBlank String name,
        String   description,
        JsonNode definition    // optional — defaults to empty graph
) {}
