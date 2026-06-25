package com.hestia.config;

import com.hestia.user.application.TokenService;
import java.util.List;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * WebGraphQlInterceptor (HES-SETUP) — request-auth seam (NOT a Spring Security filter chain).
 * Exposes:
 *   - clientIpAddress: real socket address for the login rate limiter (XFF is NOT trusted —
 *     it is client-spoofable and would let an attacker rotate it to bypass brute-force limits;
 *     trusted-proxy XFF handling is a deploy-time follow-up).
 *   - authUserId: the verified subject of a valid Bearer access token, for protected queries
 *     like `me`. Invalid/expired/absent tokens leave it unset (the resolver returns null).
 */
@Component
public class GraphQLAuthInterceptor implements WebGraphQlInterceptor {

    private final TokenService tokenService;

    public GraphQLAuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String ip = socketRemoteAddress(request);
        String userId = authUserId(request);
        request.configureExecutionInput(
                (executionInput, builder) ->
                        builder.graphQLContext(
                                        ctx -> {
                                            if (ip != null) ctx.put("clientIpAddress", ip);
                                            if (userId != null) ctx.put("authUserId", userId);
                                        })
                                .build());
        return chain.next(request);
    }

    private String authUserId(WebGraphQlRequest request) {
        List<String> auth = request.getHeaders().get("Authorization");
        if (auth == null || auth.isEmpty()) return null;
        String header = auth.get(0);
        if (header == null || !header.startsWith("Bearer ")) return null;
        try {
            return tokenService.parseUserId(header.substring(7).trim());
        } catch (RuntimeException e) {
            return null; // invalid/expired token — treated as anonymous
        }
    }

    private static String socketRemoteAddress(WebGraphQlRequest request) {
        return request.getRemoteAddress() == null
                ? null
                : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
