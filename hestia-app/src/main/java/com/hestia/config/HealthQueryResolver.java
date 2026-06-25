package com.hestia.config;

import java.util.Map;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Minimal health query for monitoring + a non-empty Query root. */
@Controller
public class HealthQueryResolver {
    @QueryMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
