package br.com.template.core.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

public abstract class CoreTestServer {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        String databaseName = "it_" + UUID.randomUUID().toString().replace("-", "");
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }
}
