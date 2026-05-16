package com.engine.dynamic.config;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ZookeeperConfigTest {

    @Test
    void testCuratorFramework() {
        ZookeeperConfig config = new ZookeeperConfig();
        ReflectionTestUtils.setField(config, "connectString", "localhost:2181");

        CuratorFramework client = config.curatorFramework();

        assertThat(client).isNotNull();
    }
}
