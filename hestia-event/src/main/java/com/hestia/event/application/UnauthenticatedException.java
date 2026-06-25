package com.hestia.event.application;

/** Thrown when a calendar operation is attempted without a valid authenticated user (TAC-1). */
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("Authentication required.");
    }
}
