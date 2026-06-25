package com.hestia.user.application;

import com.hestia.user.application.TokenService.InvalidRefreshTokenException;
import com.hestia.user.application.UserCredentialLookup.UserCredentials;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link AuthService}. Verifies the password against the stored BCrypt hash and issues
 * JWTs. Returns the SAME generic error for any credential failure (enumeration-safe, T2).
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserCredentialLookup credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthServiceImpl(
            UserCredentialLookup credentialRepository,
            BCryptPasswordEncoder passwordEncoder,
            TokenService tokenService,
            LoginRateLimiter loginRateLimiter) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    public LoginResult login(String email, String password, String ipAddress) {
        if (ipAddress != null && loginRateLimiter.isBlocked(ipAddress)) {
            log.warn("Login rate-limited for IP");
            throw new LoginRateLimiter.LoginRateLimitedException();
        }

        Optional<UserCredentials> credOpt =
                credentialRepository.findCredentialsByEmail(normalize(email));

        if (credOpt.isEmpty() || credOpt.get().passwordHash() == null) {
            if (ipAddress != null) loginRateLimiter.recordFailedAttempt(ipAddress);
            throw new AuthenticationException();
        }

        UserCredentials credentials = credOpt.get();
        if (!passwordEncoder.matches(password, credentials.passwordHash())) {
            if (ipAddress != null) loginRateLimiter.recordFailedAttempt(ipAddress);
            throw new AuthenticationException();
        }

        // Unverified accounts are rejected with the SAME generic error (enumeration-safe, T2).
        if (!credentials.emailVerified()) {
            if (ipAddress != null) loginRateLimiter.recordFailedAttempt(ipAddress);
            throw new AuthenticationException();
        }

        if (ipAddress != null) loginRateLimiter.recordSuccessfulLogin(ipAddress);

        String accessToken =
                tokenService.generateAccessToken(credentials.userId(), credentials.email());
        String refreshToken =
                tokenService.generateRefreshToken(credentials.userId(), credentials.email());
        return new LoginResult(accessToken, refreshToken, credentials.preferredLanguage());
    }

    @Override
    public RefreshResult refreshToken(String refreshToken) {
        String userId = tokenService.parseRefreshToken(refreshToken);
        Optional<UserCredentials> credOpt = credentialRepository.findCredentialsById(userId);
        if (credOpt.isEmpty()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        UserCredentials credentials = credOpt.get();
        String newAccessToken =
                tokenService.generateAccessToken(credentials.userId(), credentials.email());
        String newRefreshToken =
                tokenService.generateRefreshToken(credentials.userId(), credentials.email());
        return new RefreshResult(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken) {
        // Stateless tokens this iteration. Server-side revocation (AG2) is a documented follow-up.
        log.debug("Logout requested");
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
