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
            String path = String.format("/%s/services/%s", appEnv, serviceName);
            logger.info("Setting up ZooKeeper watch at path: {}", path);

            // Ensure path exists
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path, "{}".getBytes());
            }

            curatorCache = CuratorCache.build(curatorFramework, path);
            CuratorCacheListener listener = CuratorCacheListener.builder()
                    .forChanges((oldNode, node) -> processData(node.getData()))
                    .forCreates(node -> processData(node.getData()))
                    .forInitialized(() -> {
                        curatorCache.get(path).ifPresent(node -> processData(node.getData()));
                    })
                    .build();

            curatorCache.listenable().addListener(listener);
            curatorCache.start();
        } catch (Exception e) {
            logger.error("Failed to setup Zookeeper watcher, continuing startup.", e);
        }
    }

    private void processData(byte[] data) {
        if (data != null && data.length > 0) {
            try {
                ServiceConfig config = objectMapper.readValue(data, ServiceConfig.class);
                logger.info("Received updated config from ZooKeeper: {}", config);
                eventPublisher.publishEvent(new ServiceConfigUpdatedEvent(this, config));
            } catch (Exception e) {
                logger.error("Failed to parse ZooKeeper config payload", e);
            }
        } else {
            logger.warn("Received empty or null data from ZooKeeper path");
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
