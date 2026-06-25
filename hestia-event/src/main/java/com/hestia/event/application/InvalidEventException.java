package com.hestia.event.application;

/** Thrown when createCalendarEvent input fails server-side validation (TAC-11). */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
