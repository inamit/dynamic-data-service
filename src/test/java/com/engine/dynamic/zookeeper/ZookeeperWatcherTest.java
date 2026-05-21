package com.engine.dynamic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
    private GetChildrenBuilder getChildrenBuilder;

    @Mock
    private GetDataBuilder getDataBuilder;

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
    void processEntitiesPublishesEvent() throws Exception {
        String json = "{\"type\": \"product\", \"basePath\": \"/api/v1/products\", \"storageEngine\": \"POSTGRES\"}";

        when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath("/test/services/test-service/entities")).thenReturn(List.of("product"));

        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(getDataBuilder.forPath("/test/services/test-service/entities/product")).thenReturn(json.getBytes());

        // Use reflection to call the private processEntities method
        ReflectionTestUtils.invokeMethod(watcher, "processEntities", "/test/services/test-service/entities");

        ArgumentCaptor<ServiceConfigUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ServiceConfigUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ServiceConfig config = eventCaptor.getValue().getConfig();
        assertThat(config.entities()).hasSize(1);
        assertThat(config.entities().get(0).type()).isEqualTo("product");
        assertThat(config.entities().get(0).basePath()).isEqualTo("/api/v1/products");
    }

    @Test
    void processEntitiesInfersValuesFromNodeNameEmptyData() throws Exception {
        when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath("/test/services/test-service/entities")).thenReturn(List.of("category"));

        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(getDataBuilder.forPath("/test/services/test-service/entities/category")).thenReturn(new byte[0]);

        ReflectionTestUtils.invokeMethod(watcher, "processEntities", "/test/services/test-service/entities");

        ArgumentCaptor<ServiceConfigUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ServiceConfigUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ServiceConfig config = eventCaptor.getValue().getConfig();
        assertThat(config.entities()).hasSize(1);
        assertThat(config.entities().get(0).type()).isEqualTo("category");
        assertThat(config.entities().get(0).basePath()).isEqualTo("/api/v1/category");
        assertThat(config.entities().get(0).storageEngine()).isEqualTo("POSTGRES");
    }

    @Test
    void processEntitiesInfersValuesFromNodeNameEmptyJson() throws Exception {
        when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath("/test/services/test-service/entities")).thenReturn(List.of("user"));

        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(getDataBuilder.forPath("/test/services/test-service/entities/user")).thenReturn("{}".getBytes());

        ReflectionTestUtils.invokeMethod(watcher, "processEntities", "/test/services/test-service/entities");

        ArgumentCaptor<ServiceConfigUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ServiceConfigUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ServiceConfig config = eventCaptor.getValue().getConfig();
        assertThat(config.entities()).hasSize(1);
        assertThat(config.entities().get(0).type()).isEqualTo("user");
        assertThat(config.entities().get(0).basePath()).isEqualTo("/api/v1/user");
        assertThat(config.entities().get(0).storageEngine()).isEqualTo("POSTGRES");
    }

    @Test
    void processEntitiesHandlesInvalidJson() throws Exception {
        when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath("/test/services/test-service/entities")).thenReturn(List.of("product"));

        when(curatorFramework.getData()).thenReturn(getDataBuilder);
        when(getDataBuilder.forPath("/test/services/test-service/entities/product")).thenReturn("invalid".getBytes());

        ReflectionTestUtils.invokeMethod(watcher, "processEntities", "/test/services/test-service/entities");

        ArgumentCaptor<ServiceConfigUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ServiceConfigUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ServiceConfig config = eventCaptor.getValue().getConfig();
        assertThat(config.entities()).hasSize(1);
        assertThat(config.entities().get(0).type()).isEqualTo("product"); // Falls back to node name
    }

    @Test
    void processEntitiesHandlesException() throws Exception {
        when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath("/test/services/test-service/entities")).thenThrow(new RuntimeException("Test"));

        ReflectionTestUtils.invokeMethod(watcher, "processEntities", "/test/services/test-service/entities");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void stopWatchingCatchesException() {
        ReflectionTestUtils.setField(watcher, "curatorCache", mock(org.apache.curator.framework.recipes.cache.CuratorCache.class));
        doThrow(new RuntimeException("Test")).when((org.apache.curator.framework.recipes.cache.CuratorCache) ReflectionTestUtils.getField(watcher, "curatorCache")).close();
        watcher.stopWatching(); // should catch exception
    }

    @Test
    void stopWatchingNullCache() {
        watcher.stopWatching(); // should do nothing
    }
}
