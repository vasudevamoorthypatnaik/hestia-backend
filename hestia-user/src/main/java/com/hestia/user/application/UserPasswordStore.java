package com.hestia.user.application;

import java.util.UUID;

/** Port: create a new user with a hashed password. */
public interface UserPasswordStore {

    /** Persist a new user. Throws {@link EmailAlreadyExistsException} on duplicate email. */
    UUID createUser(String email, String firstName, String lastName, String passwordHash);

    /** Thrown when registering an email that already exists (case-insensitive, T6). */
    class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException() {
            super("Email already registered");
        }
    }
}
