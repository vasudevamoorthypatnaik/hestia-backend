package com.hestia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Hestia Backend — GraphQL API entrypoint (HES-SETUP). DDD/hexagonal, schema-first GraphQL,
 * PostgreSQL + Flyway. Component-scans all com.hestia modules; auth lives in hestia-user.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.hestia.*.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.hestia.*.infrastructure.persistence")
public class HestiaBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HestiaBackendApplication.class, args);
    }
}
