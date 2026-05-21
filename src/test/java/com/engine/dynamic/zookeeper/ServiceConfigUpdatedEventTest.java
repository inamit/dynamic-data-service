package com.engine.dynamic.zookeeper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceConfigUpdatedEventTest {

    @Test
    void testEvent() {
        ServiceConfig config = new ServiceConfig(List.of());
        ServiceConfigUpdatedEvent event = new ServiceConfigUpdatedEvent(this, config);

        assertThat(event.getConfig()).isEqualTo(config);
        assertThat(event.getSource()).isEqualTo(this);
    }
}
