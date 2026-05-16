package com.engine.dynamic.zookeeper;

import org.springframework.context.ApplicationEvent;

public class ServiceConfigUpdatedEvent extends ApplicationEvent {
    private final ServiceConfig config;

    public ServiceConfigUpdatedEvent(Object source, ServiceConfig config) {
        super(source);
        this.config = config;
    }

    public ServiceConfig getConfig() {
        return config;
    }
}
