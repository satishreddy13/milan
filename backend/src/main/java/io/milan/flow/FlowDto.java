package io.milan.flow;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record FlowDto(
        UUID          id,
        String        name,
        String        description,
        FlowStatus    status,
        JsonNode      definition,   // returned as a JSON object, not a string
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
