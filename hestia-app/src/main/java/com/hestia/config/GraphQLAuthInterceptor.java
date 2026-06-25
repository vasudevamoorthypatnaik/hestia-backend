package com.hestia.config;

import java.util.List;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * WebGraphQlInterceptor (HES-SETUP) — exposes the client IP to resolvers via GraphQL context
 * (used by the login rate limiter). This is the request-auth seam (NOT a Spring Security filter).
 * JWT verification for protected operations is a follow-up.
 */
@Component
public class GraphQLAuthInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String ip = clientIp(request);
        if (ip != null) {
            request.configureExecutionInput(
                    (executionInput, builder) ->
                            builder.graphQLContext(ctx -> ctx.put("clientIpAddress", ip)).build());
        }
        return chain.next(request);
    }

    private static String clientIp(WebGraphQlRequest request) {
        List<String> forwarded = request.getHeaders().get("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.get(0).split(",")[0].trim();
        }
        return request.getRemoteAddress() == null
                ? null
                : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
