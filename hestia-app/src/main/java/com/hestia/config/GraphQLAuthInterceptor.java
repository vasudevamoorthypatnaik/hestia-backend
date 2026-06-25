package com.hestia.config;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * WebGraphQlInterceptor (HES-SETUP) — exposes the client IP to resolvers via GraphQL context
 * (used by the login rate limiter). Request-auth seam (NOT a Spring Security filter); JWT
 * verification for protected operations is a follow-up.
 *
 * <p>SECURITY: the rate-limit key uses the real socket remote address, NOT the client-supplied
 * {@code X-Forwarded-For} header — trusting XFF here would let an attacker rotate a spoofed
 * header value to mint a fresh rate-limit bucket per request and bypass brute-force protection.
 * When deployed behind a TRUSTED proxy/load balancer, XFF handling must be reintroduced behind
 * an explicit trusted-proxy allowlist (deploy-time follow-up), not trusted unconditionally.
 */
@Component
public class GraphQLAuthInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String ip = socketRemoteAddress(request);
        if (ip != null) {
            request.configureExecutionInput(
                    (executionInput, builder) ->
                            builder.graphQLContext(ctx -> ctx.put("clientIpAddress", ip)).build());
        }
        return chain.next(request);
    }

    private static String socketRemoteAddress(WebGraphQlRequest request) {
        return request.getRemoteAddress() == null
                ? null
                : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
