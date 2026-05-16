package com.engine.dynamic.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicPayloadTest {

    @Test
    void testDynamicPayload() {
        ObjectMapper mapper = new ObjectMapper();
        DynamicPayload payload = new DynamicPayload(1L, "test", mapper.createObjectNode().put("key", "value"));

        assertThat(payload.id()).isEqualTo(1L);
        assertThat(payload.entityType()).isEqualTo("test");
        assertThat(payload.payload().get("key").asText()).isEqualTo("value");
    }
}
