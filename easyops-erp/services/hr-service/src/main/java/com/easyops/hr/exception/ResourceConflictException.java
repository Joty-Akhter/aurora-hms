package com.easyops.hr.exception;

/**
 * Thrown when a resource would violate a uniqueness constraint (e.g. duplicate code).
 * Mapped to HTTP 409 Conflict by {@link com.easyops.hr.config.HrExceptionHandler}.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
