package com.hestia.user.application;

/** Authentication use cases: login, refresh, logout. */
public interface AuthService {

    record LoginResult(String accessToken, String refreshToken, String preferredLanguage) {}

    record RefreshResult(String accessToken, String refreshToken) {}

    LoginResult login(String email, String password, String ipAddress);

    RefreshResult refreshToken(String refreshToken);

    void logout(String accessToken);

    /** Generic credential failure — same message for all causes (enumeration-safe, T2). */
    class AuthenticationException extends RuntimeException {
        public AuthenticationException() {
            super("Invalid email or password");
        }
    }
}
