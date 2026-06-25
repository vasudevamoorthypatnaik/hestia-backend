package com.hestia.user.application;

/** Registration use case. */
public interface RegistrationService {

    record RegisterResult(String message, String userId, int ttlMinutes, String preferredLanguage) {}

    RegisterResult register(String email, String password, String firstName, String lastName);
}
