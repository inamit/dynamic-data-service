package com.engine.dynamic.zookeeper;

import java.util.List;

public record ServiceConfig(List<EntityConfig> entities) {
    public record EntityConfig(String type, String basePath, String storageEngine) {
    }
}
