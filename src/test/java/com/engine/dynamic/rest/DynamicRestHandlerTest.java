package com.engine.dynamic.rest;

import com.engine.dynamic.model.DynamicPayload;
import com.engine.dynamic.repository.DynamicStorageRepository;
import com.engine.dynamic.zookeeper.ServiceConfig;
import com.engine.dynamic.zookeeper.ServiceConfigUpdatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicRestHandlerTest {

    @Mock
    private DynamicStorageRepository storageRepository;

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    @InjectMocks
    private DynamicRestHandler handler;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ServiceConfig config = new ServiceConfig(List.of(
                new ServiceConfig.EntityConfig("product", "/api/v1/products", "POSTGRES")
        ));
        handler.onApplicationEvent(new ServiceConfigUpdatedEvent(this, config));
    }

    @Test
    void testHandlePost() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/products");
        JsonNode payload = objectMapper.readTree("{\"name\": \"Test\"}");

        when(storageRepository.save(eq("product"), any(DynamicPayload.class))).thenReturn(1L);

        ResponseEntity<DynamicPayload> response = handler.handlePost(request, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().entityType()).isEqualTo("product");
        assertThat(response.getBody().payload()).isEqualTo(payload);
    }

    @Test
    void testHandleGet() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/1");
        JsonNode payload = objectMapper.createObjectNode().put("name", "Test");
        DynamicPayload expected = new DynamicPayload(1L, "product", payload);

        when(storageRepository.findById("product", 1L)).thenReturn(Optional.of(expected));

        ResponseEntity<DynamicPayload> response = handler.handleGet(request, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void testHandlePut() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/products/1");
        JsonNode payload = objectMapper.readTree("{\"name\": \"Updated\"}");

        when(storageRepository.findById("product", 1L)).thenReturn(Optional.of(new DynamicPayload(1L, "product", objectMapper.createObjectNode())));
        when(storageRepository.save(eq("product"), any(DynamicPayload.class))).thenReturn(1L);

        ResponseEntity<DynamicPayload> response = handler.handlePut(request, 1L, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().payload()).isEqualTo(payload);
    }

    @Test
    void testHandleDelete() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/products/1");

        ResponseEntity<Void> response = handler.handleDelete(request, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(storageRepository).deleteById("product", 1L);
    }
}
