package com.hestia.user.infrastructure.persistence;

import com.hestia.user.application.UserCredentialLookup;
import com.hestia.user.application.UserPasswordStore;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** JPA adapter implementing the credential-lookup + password-store ports. */
@Repository
public class JpaUserAdapter implements UserCredentialLookup, UserPasswordStore {

    private final UserJpaRepository repository;

    public JpaUserAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(this::toCredentials);
    }

    @Override
    public Optional<UserCredentials> findCredentialsById(String userId) {
        try {
            return repository.findById(UUID.fromString(userId)).map(this::toCredentials);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public UUID createUser(String email, String firstName, String lastName, String passwordHash) {
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }
        UUID id = UUID.randomUUID();
        try {
            repository.save(
                    new UserEntity(
                            id, email, firstName, lastName, passwordHash, true, "en", Instant.now()));
        } catch (DataIntegrityViolationException e) {
            // Unique constraint race (T6).
            throw new EmailAlreadyExistsException();
        }
        return id;
    }

    private UserCredentials toCredentials(UserEntity e) {
        return new UserCredentials(
                e.getId().toString(),
                e.getEmail(),
                e.getPasswordHash(),
                e.isEmailVerified(),
                e.getPreferredLanguage() == null ? "en" : e.getPreferredLanguage());
    }
}
