package com.hestia.user.application;

import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link RegistrationService}. Hashes the password with BCrypt (T1) and persists the
 * user. Email uniqueness (case-insensitive) is enforced by the store (T6).
 */
@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final UserPasswordStore userStore;
    private final BCryptPasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(
            UserPasswordStore userStore, BCryptPasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResult register(
            String email, String password, String firstName, String lastName) {
        String hash = passwordEncoder.encode(password);
        UUID userId =
                userStore.createUser(normalize(email), firstName, lastName, hash);
        // Email verification is a follow-up; users are created verified for the MVP.
        return new RegisterResult("Registration successful.", userId.toString(), 0, "en");
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
