package com.engine.dynamic.rest;

import com.engine.dynamic.model.DynamicPayload;
import com.engine.dynamic.repository.DynamicStorageRepository;
import com.engine.dynamic.zookeeper.ServiceConfig;
import com.engine.dynamic.zookeeper.ServiceConfigUpdatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class DynamicRestHandler implements ApplicationListener<ServiceConfigUpdatedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DynamicRestHandler.class);

    private final DynamicStorageRepository storageRepository;
    private final RequestMappingHandlerMapping handlerMapping;
    private final Map<String, RequestMappingInfo> currentMappings = new HashMap<>();
    private final Map<String, String> pathToEntityTypeMap = new HashMap<>();

    public DynamicRestHandler(DynamicStorageRepository storageRepository, RequestMappingHandlerMapping handlerMapping) {
        this.storageRepository = storageRepository;
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ServiceConfigUpdatedEvent event) {
        ServiceConfig config = event.getConfig();
        updateRoutes(config);
    }

    private synchronized void updateRoutes(ServiceConfig config) {
        // Unregister existing routes
        for (RequestMappingInfo info : currentMappings.values()) {
            handlerMapping.unregisterMapping(info);
        }
        currentMappings.clear();
        pathToEntityTypeMap.clear();

        if (config == null || config.entities() == null) {
            return;
        }

        try {
            Method createMethod = this.getClass().getMethod("handlePost", HttpServletRequest.class, JsonNode.class);
            Method getMethod = this.getClass().getMethod("handleGet", HttpServletRequest.class, Long.class);
            Method putMethod = this.getClass().getMethod("handlePut", HttpServletRequest.class, Long.class, JsonNode.class);
            Method deleteMethod = this.getClass().getMethod("handleDelete", HttpServletRequest.class, Long.class);

            // Use Spring Boot 3 PathPatternParser approach for RequestMappingInfo builder
            RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
            options.setPatternParser(new PathPatternParser());

            for (ServiceConfig.EntityConfig entity : config.entities()) {
                String basePath = entity.basePath();
                String entityType = entity.type();
                logger.info("Registering routes for entity type: {} at {}", entityType, basePath);

                pathToEntityTypeMap.put(basePath, entityType);

                // POST {basePath}
                RequestMappingInfo postInfo = RequestMappingInfo.paths(basePath)
                        .methods(org.springframework.web.bind.annotation.RequestMethod.POST)
                        .options(options)
                        .build();
                handlerMapping.registerMapping(postInfo, this, createMethod);
                currentMappings.put(basePath + "-POST", postInfo);

                // GET {basePath}/{id}
                RequestMappingInfo getInfo = RequestMappingInfo.paths(basePath + "/{id}")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.GET)
                        .options(options)
                        .build();
                handlerMapping.registerMapping(getInfo, this, getMethod);
                currentMappings.put(basePath + "-GET", getInfo);

                // PUT {basePath}/{id}
                RequestMappingInfo putInfo = RequestMappingInfo.paths(basePath + "/{id}")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.PUT)
                        .options(options)
                        .build();
                handlerMapping.registerMapping(putInfo, this, putMethod);
                currentMappings.put(basePath + "-PUT", putInfo);

                // DELETE {basePath}/{id}
                RequestMappingInfo deleteInfo = RequestMappingInfo.paths(basePath + "/{id}")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.DELETE)
                        .options(options)
                        .build();
                handlerMapping.registerMapping(deleteInfo, this, deleteMethod);
                currentMappings.put(basePath + "-DELETE", deleteInfo);
            }
        } catch (NoSuchMethodException e) {
            logger.error("Failed to find handler methods for dynamic routing", e);
        }
    }

    private String getEntityType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String basePath : pathToEntityTypeMap.keySet()) {
            if (uri.equals(basePath) || uri.startsWith(basePath + "/")) {
                return pathToEntityTypeMap.get(basePath);
            }
        }
        throw new IllegalStateException("Unknown entity for URI: " + uri);
    }

    @ResponseBody
    public ResponseEntity<DynamicPayload> handlePost(HttpServletRequest request, @RequestBody JsonNode payload) {
        String entityType = getEntityType(request);
        DynamicPayload toSave = new DynamicPayload(null, entityType, payload);
        Long id = storageRepository.save(entityType, toSave);
        DynamicPayload saved = new DynamicPayload(id, entityType, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @ResponseBody
    public ResponseEntity<DynamicPayload> handleGet(HttpServletRequest request, @PathVariable("id") Long id) {
        String entityType = getEntityType(request);
        Optional<DynamicPayload> payload = storageRepository.findById(entityType, id);
        return payload.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ResponseBody
    public ResponseEntity<DynamicPayload> handlePut(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody JsonNode payload) {
        String entityType = getEntityType(request);

        Optional<DynamicPayload> existing = storageRepository.findById(entityType, id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DynamicPayload toUpdate = new DynamicPayload(id, entityType, payload);
        storageRepository.save(entityType, toUpdate);
        return ResponseEntity.ok(toUpdate);
    }

    @ResponseBody
    public ResponseEntity<Void> handleDelete(HttpServletRequest request, @PathVariable("id") Long id) {
        String entityType = getEntityType(request);
        storageRepository.deleteById(entityType, id);
        return ResponseEntity.noContent().build();
    }
}
