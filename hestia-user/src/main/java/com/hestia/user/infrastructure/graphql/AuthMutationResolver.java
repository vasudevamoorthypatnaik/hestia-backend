package com.hestia.user.infrastructure.graphql;

import com.hestia.user.application.AuthService;
import com.hestia.user.application.AuthService.AuthenticationException;
import com.hestia.user.application.AuthService.LoginResult;
import com.hestia.user.application.AuthService.RefreshResult;
import com.hestia.user.application.LoginRateLimiter;
import com.hestia.user.application.RegistrationService;
import com.hestia.user.application.RegistrationService.RegisterResult;
import com.hestia.user.application.TokenService.InvalidRefreshTokenException;
import com.hestia.user.application.UserPasswordStore.EmailAlreadyExistsException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.Objects;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * GraphQL auth mutations (HES-SETUP): login / registerUser / refreshToken / logout.
 * All credential failures map to a single generic UNAUTHORIZED (enumeration-safe, T2).
 */
@Controller
public class AuthMutationResolver {

    private final AuthService authService;
    private final RegistrationService registrationService;

    public AuthMutationResolver(
            AuthService authService, RegistrationService registrationService) {
        this.authService = authService;
        this.registrationService = registrationService;
    }

    record LoginInput(String email, String password, String captchaToken) {
        LoginInput {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(password, "password is required");
        }
    }

    record RegisterUserInput(
            String email, String password, String firstName, String lastName) {
        RegisterUserInput {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(password, "password is required");
        }
    }

    record RefreshTokenInput(String refreshToken) {
        RefreshTokenInput {
            Objects.requireNonNull(refreshToken, "refreshToken is required");
        }
    }

    @MutationMapping
    public Map<String, String> login(
            @Argument LoginInput input,
            @ContextValue(name = "clientIpAddress", required = false) String clientIp) {
        LoginResult result = authService.login(input.email(), input.password(), clientIp);
        return Map.of(
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken(),
                "preferredLanguage", result.preferredLanguage());
    }

    @MutationMapping
    public Map<String, Object> registerUser(@Argument RegisterUserInput input) {
        RegisterResult result =
                registrationService.register(
                        input.email(),
                        input.password(),
                        input.firstName() == null ? "" : input.firstName(),
                        input.lastName() == null ? "" : input.lastName());
        return Map.of(
                "message", result.message(),
                "userId", result.userId(),
                "ttlMinutes", result.ttlMinutes(),
                "preferredLanguage", result.preferredLanguage());
    }

    @MutationMapping
    public Map<String, String> refreshToken(@Argument RefreshTokenInput input) {
        RefreshResult result = authService.refreshToken(input.refreshToken());
        return Map.of(
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken());
    }

    @MutationMapping
    public Map<String, Object> logout() {
        authService.logout(null);
        return Map.of("success", true);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleAuthentication(
            AuthenticationException ex, DataFetchingEnvironment env) {
        return error(ErrorType.UNAUTHORIZED, "Invalid email or password", env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidRefresh(
            InvalidRefreshTokenException ex, DataFetchingEnvironment env) {
        return error(ErrorType.UNAUTHORIZED, "Invalid refresh token", env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleRateLimited(
            LoginRateLimiter.LoginRateLimitedException ex, DataFetchingEnvironment env) {
        return error(ErrorType.FORBIDDEN, ex.getMessage(), env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleEmailExists(
            EmailAlreadyExistsException ex, DataFetchingEnvironment env) {
        return error(ErrorType.BAD_REQUEST, "Email already registered", env);
    }

    private static GraphQLError error(
            ErrorType type, String message, DataFetchingEnvironment env) {
        return GraphQLError.newError()
                .errorType(type)
                .message(message)
                .extensions(Map.of("userMessage", message))
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}
