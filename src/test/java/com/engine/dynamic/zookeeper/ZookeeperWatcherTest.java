package com.engine.dynamic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZookeeperWatcherTest {

    @Mock
    private CuratorFramework curatorFramework;

    @Mock
    private ExistsBuilder existsBuilder;

    @Mock
    private CreateBuilder createBuilder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ZookeeperWatcher watcher;

    @BeforeEach
    void setUp() throws Exception {
        watcher = new ZookeeperWatcher(curatorFramework, objectMapper, eventPublisher);
        ReflectionTestUtils.setField(watcher, "appEnv", "test");
        ReflectionTestUtils.setField(watcher, "serviceName", "test-service");
    }

    @Test
    void processDataPublishesEvent() throws Exception {
        String json = "{\"serviceName\": \"test-service\", \"entities\": [{\"type\": \"product\", \"basePath\": \"/api/v1/products\", \"storageEngine\": \"POSTGRES\"}]}";

        // Use reflection to call the private processData method
        ReflectionTestUtils.invokeMethod(watcher, "processData", json.getBytes());

        ArgumentCaptor<ServiceConfigUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ServiceConfigUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ServiceConfig config = eventCaptor.getValue().getConfig();
        assertThat(config.serviceName()).isEqualTo("test-service");
        assertThat(config.entities()).hasSize(1);
        assertThat(config.entities().get(0).type()).isEqualTo("product");
    }

    @Test
    void processDataHandlesEmpty() throws Exception {
        ReflectionTestUtils.invokeMethod(watcher, "processData", new byte[0]);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void processDataHandlesInvalidJson() throws Exception {
        ReflectionTestUtils.invokeMethod(watcher, "processData", "invalid".getBytes());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
