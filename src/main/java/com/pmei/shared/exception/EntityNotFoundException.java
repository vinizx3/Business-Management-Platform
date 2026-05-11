package com.pmei.shared.exception;

/**
 * Exception thrown when an entity is not found.
 *
 * Can be used for database-related errors.
 */
public class EntityNotFoundException extends RuntimeException{

    public EntityNotFoundException(String message) {
        super(message);
    }
}
