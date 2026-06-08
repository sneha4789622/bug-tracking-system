package com.bugtracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ResourceNotFoundException
 *
 * Thrown when a requested resource (Project, Bug, User, etc.)
 * does not exist in the system.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) tells Spring to return
 * HTTP 404 when this exception is not caught by a controller.
 *
 * We extend RuntimeException (unchecked) rather than Exception (checked)
 * because callers should not be forced to declare or catch it — Spring
 * handles it globally via the exception handler we create next.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message describes what was not found, e.g.,
     *                "Project not found with ID: 5"
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Use this constructor when you have an underlying cause.
     *
     * @param message descriptive error message
     * @param cause   the original exception that caused this
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

