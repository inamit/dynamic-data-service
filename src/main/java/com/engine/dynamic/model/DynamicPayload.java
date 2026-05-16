package com.engine.dynamic.model;

import com.fasterxml.jackson.databind.JsonNode;

public record DynamicPayload(Long id, String entityType, JsonNode payload) {
}
