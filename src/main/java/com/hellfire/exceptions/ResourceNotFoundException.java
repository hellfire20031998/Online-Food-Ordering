package com.hellfire.exceptions;

/** Generic 404 for team-console lookups (users, orders, settings). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
