package com.hestia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for the auth mutations against a real Postgres (Testcontainers) and the full
 * Spring context. Covers: register → login (success), wrong password (enumeration-safe), and
 * input validation. (T12 / U6 / T2)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Testcontainers
class AuthMutationIntegrationTest {

    // The hestia-notification module wires an AWS SqsAsyncClient that needs a region/creds at
    // bean-init. Provide dummy SDK system settings before the Spring context loads.
    static {
        System.setProperty("aws.region", "us-east-1");
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16").withDatabaseName("hestia");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("hestia.jwt.secret", () -> "test-signing-secret-key-at-least-32-bytes-long");
    }

    @Autowired private HttpGraphQlTester graphQlTester;

    private static final String PASSWORD = "password123";

    private void register(String email) {
        graphQlTester
                .document(
                        "mutation($i:RegisterUserInput!){registerUser(input:$i){userId message}}")
                .variable(
                        "i",
                        java.util.Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "firstName", "I",
                                "lastName", "Test"))
                .execute()
                .path("registerUser.userId")
                .entity(String.class)
                .satisfies(id -> assertThat(id).isNotBlank());
    }

    @Test
    void registerThenLogin_returnsTokens() {
        String email = "itest-login@hestia.app";
        register(email);
        graphQlTester
                .document("mutation($i:LoginInput!){login(input:$i){accessToken refreshToken preferredLanguage}}")
                .variable("i", java.util.Map.of("email", email, "password", PASSWORD))
                .execute()
                .path("login.accessToken")
                .entity(String.class)
                .satisfies(t -> assertThat(t).contains("."))
                .path("login.preferredLanguage")
                .entity(String.class)
                .isEqualTo("en");
    }

    @Test
    void login_wrongPassword_isGenericUnauthorized() {
        String email = "itest-wrong@hestia.app";
        register(email);
        graphQlTester
                .document("mutation($i:LoginInput!){login(input:$i){accessToken}}")
                .variable("i", java.util.Map.of("email", email, "password", "wrongpassword"))
                .execute()
                .errors()
                .satisfy(
                        errors -> {
                            assertThat(errors).isNotEmpty();
                            assertThat(errors.get(0).getMessage())
                                    .isEqualTo("Invalid email or password");
                        });
    }

    @Test
    void login_nonexistentEmail_sameGenericError() {
        graphQlTester
                .document("mutation($i:LoginInput!){login(input:$i){accessToken}}")
                .variable("i", java.util.Map.of("email", "nobody@hestia.app", "password", "password123"))
                .execute()
                .errors()
                .satisfy(
                        errors ->
                                assertThat(errors.get(0).getMessage())
                                        .isEqualTo("Invalid email or password"));
    }
}
