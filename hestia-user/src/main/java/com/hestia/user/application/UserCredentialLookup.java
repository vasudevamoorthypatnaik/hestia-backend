package com.hestia.user.application;

import java.util.Optional;

/** Port: read user credentials for authentication. */
public interface UserCredentialLookup {

    /** Credentials needed to authenticate + issue tokens. */
    record UserCredentials(
            String userId,
            String email,
            String passwordHash,
            boolean emailVerified,
            String preferredLanguage) {}

    Optional<UserCredentials> findCredentialsByEmail(String email);

    Optional<UserCredentials> findCredentialsById(String userId);
}
