package com.engine.dynamic.zookeeper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceConfigTest {

    @Test
    void testServiceConfig() {
        ServiceConfig.EntityConfig entityConfig = new ServiceConfig.EntityConfig("product", "/api/v1/products", "POSTGRES");
        ServiceConfig config = new ServiceConfig(List.of(entityConfig));

        assertThat(config.entities()).hasSize(1);

        ServiceConfig.EntityConfig retrieved = config.entities().get(0);
        assertThat(retrieved.type()).isEqualTo("product");
        assertThat(retrieved.basePath()).isEqualTo("/api/v1/products");
        assertThat(retrieved.storageEngine()).isEqualTo("POSTGRES");
    }
}
