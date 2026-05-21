package com.engine.dynamic.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ZookeeperWatcher {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperWatcher.class);

    private final CuratorFramework curatorFramework;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.env:dev}")
    private String appEnv;

    @Value("${SERVICE_NAME:inventory}")
    private String serviceName;

    private CuratorCache curatorCache;

    public ZookeeperWatcher(CuratorFramework curatorFramework, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.curatorFramework = curatorFramework;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void startWatching() {
        try {
            // Updated to the new structure: /${appEnv}/services/${serviceName}/entities
            String path = String.format("/%s/services/%s/entities", appEnv, serviceName);
            logger.info("Setting up ZooKeeper watch at path: {}", path);

            // Ensure path exists
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path, new byte[0]);
            }

            curatorCache = CuratorCache.build(curatorFramework, path);
            CuratorCacheListener listener = CuratorCacheListener.builder()
                    .forChanges((oldNode, node) -> processEntities(path))
                    .forCreates(node -> processEntities(path))
                    .forDeletes(node -> processEntities(path))
                    .forInitialized(() -> processEntities(path))
                    .build();

            curatorCache.listenable().addListener(listener);
            curatorCache.start();
        } catch (Exception e) {
            logger.error("Failed to setup Zookeeper watcher, continuing startup.", e);
        }
    }

    private void processEntities(String basePath) {
        try {
            List<String> children = curatorFramework.getChildren().forPath(basePath);
            List<ServiceConfig.EntityConfig> entities = new ArrayList<>();

            for (String child : children) {
                String childPath = basePath + "/" + child;
                byte[] data = curatorFramework.getData().forPath(childPath);

                String type = child;
                String endpointPath = "/api/v1/" + child;
                String storageEngine = "POSTGRES";

                if (data != null && data.length > 0) {
                    try {
                        Map<String, String> configMap = objectMapper.readValue(data, Map.class);
                        if (configMap.containsKey("type")) {
                            type = configMap.get("type");
                        }
                        if (configMap.containsKey("basePath")) {
                            endpointPath = configMap.get("basePath");
                        }
                        if (configMap.containsKey("storageEngine")) {
                            storageEngine = configMap.get("storageEngine");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse entity config for {}, using defaults", childPath, e);
                    }
                }

                ServiceConfig.EntityConfig entityConfig = new ServiceConfig.EntityConfig(type, endpointPath, storageEngine);
                entities.add(entityConfig);
            }

            ServiceConfig config = new ServiceConfig(entities);
            logger.info("Received updated config from ZooKeeper: {}", config);
            eventPublisher.publishEvent(new ServiceConfigUpdatedEvent(this, config));
        } catch (Exception e) {
            logger.error("Failed to list and parse Zookeeper children at {}", basePath, e);
        }
    }

    @PreDestroy
    public void stopWatching() {
        if (curatorCache != null) {
            try {
                curatorCache.close();
            } catch (Exception e) {
                logger.error("Error closing CuratorCache", e);
            }
        }
    }
}
