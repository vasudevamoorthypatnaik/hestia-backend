package com.hestia.user.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory per-IP login rate limiter (HES-SETUP). Per-instance only (resets on restart, not
 * multi-instance safe) — acceptable for local; production hardening is follow-up.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempts(int count, Instant windowStart) {}

    private final Map<String, Attempts> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Attempts a = failures.get(ip);
        if (a == null) return false;
        if (Instant.now().isAfter(a.windowStart().plus(WINDOW))) {
            failures.remove(ip);
            return false;
        }
        return a.count() >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailedAttempt(String ip) {
        failures.compute(
                ip,
                (k, a) -> {
                    Instant now = Instant.now();
                    if (a == null || now.isAfter(a.windowStart().plus(WINDOW))) {
                        return new Attempts(1, now);
                    }
                    return new Attempts(a.count() + 1, a.windowStart());
                });
    }

    public void recordSuccessfulLogin(String ip) {
        failures.remove(ip);
    }

    public static class LoginRateLimitedException extends RuntimeException {
        public LoginRateLimitedException() {
            super("Too many login attempts. Please try again later.");
        }
    }
}
