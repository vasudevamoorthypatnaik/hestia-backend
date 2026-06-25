package com.hestia.user.application;

/** Port: JWT access/refresh token generation + parsing. */
public interface TokenService {

    String generateAccessToken(String userId, String email);

    String generateRefreshToken(String userId, String email);

    /** Parse + validate an access token, returning the subject (userId). */
    String parseUserId(String token);

    /** Parse + validate a refresh token (type=refresh), returning the subject (userId). */
    String parseRefreshToken(String token);

    /** Thrown when a refresh token is invalid/expired/wrong-type. */
    class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) {
            super(message);
        }
    }
}
