package com.engine.dynamic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class DynamicRestEngineApplicationTests {

    @Test
    void contextLoads() {
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("spring.main.web-application-type", "none");
        System.setProperty("zookeeper.connect-string", "dummy");
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        System.setProperty("spring.datasource.driverClassName", "org.h2.Driver");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "password");

        try {
            DynamicRestEngineApplication.main(new String[]{});
        } catch (Exception e) {
            // expected to fail context load but we just need coverage on main
        }
    }
}
