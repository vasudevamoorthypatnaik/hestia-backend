package com.hestia.user.infrastructure.jwt;

import com.hestia.user.application.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JJWT-based {@link TokenService}. Fail-closed: refuses to construct if the signing key is absent
 * or shorter than 32 chars / 256 bits (AG1). Access tokens are short-lived; refresh tokens long.
 */
@Service
public class JwtTokenService implements TokenService {

    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtTokenService(
            @Value("${hestia.jwt.secret:}") String secret,
            @Value("${hestia.jwt.access-ttl-minutes:60}") long accessTtlMinutes,
            @Value("${hestia.jwt.refresh-ttl-days:30}") long refreshTtlDays) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "hestia.jwt.secret is missing or too weak (min 32 chars / 256 bits). "
                            + "Set HESTIA_JWT_SECRET — the app refuses to start without it (AG1).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    @Override
    public String generateAccessToken(String userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(EMAIL_CLAIM, email)
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    @Override
    public String generateRefreshToken(String userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(EMAIL_CLAIM, email)
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtlDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    @Override
    public String parseUserId(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Not an access token");
        }
        return claims.getSubject();
    }

    @Override
    public String parseRefreshToken(String token) {
        try {
            Claims claims = parse(token);
            if (!TYPE_REFRESH.equals(claims.get(TYPE_CLAIM, String.class))) {
                throw new InvalidRefreshTokenException("Not a refresh token");
            }
            return claims.getSubject();
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
