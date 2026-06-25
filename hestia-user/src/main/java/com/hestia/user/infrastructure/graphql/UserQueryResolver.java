package com.hestia.user.infrastructure.graphql;

import com.hestia.user.application.UserQueryService;
import com.hestia.user.application.UserQueryService.MeView;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** `me` query — returns the authenticated user, or null when there is no/invalid token. */
@Controller
public class UserQueryResolver {

    private final UserQueryService userQueryService;

    public UserQueryResolver(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @QueryMapping
    public Map<String, Object> me(
            @ContextValue(name = "authUserId", required = false) String userId) {
        if (userId == null) {
            return null; // anonymous — frontend pauses this query without a token (T15)
        }
        return userQueryService
                .findById(userId)
                .map(this::toMap)
                .orElse(null);
    }

    private Map<String, Object> toMap(MeView m) {
        return Map.of(
                "id", m.id(),
                "email", m.email(),
                "firstName", m.firstName() == null ? "" : m.firstName(),
                "lastName", m.lastName() == null ? "" : m.lastName(),
                "preferredLanguage", m.preferredLanguage());
    }
}
