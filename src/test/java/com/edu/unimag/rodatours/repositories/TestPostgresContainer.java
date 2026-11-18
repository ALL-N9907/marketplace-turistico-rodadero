package com.edu.unimag.rodatours.repositories;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Contenedor PostgreSQL único para toda la JVM de tests.
 * Evita iniciar y detener una instancia por cada clase de test.
 */
public final class TestPostgresContainer {

    private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        CONTAINER.start();
    }

    private TestPostgresContainer() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
