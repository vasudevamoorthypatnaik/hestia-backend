package com.hestia.user.application;

/** Thrown when auth input fails length/policy validation. Mapped to BAD_REQUEST. */
public class InputValidationException extends RuntimeException {
    public InputValidationException(String message) {
        super(message);
    }
}
