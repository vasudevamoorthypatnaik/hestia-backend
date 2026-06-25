package com.hestia.user.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * User domain model (HES-SETUP). Minimal email/password identity for the MVP login.
 */
public record User(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String passwordHash,
        boolean emailVerified,
        String preferredLanguage,
        Instant createdAt) {}
