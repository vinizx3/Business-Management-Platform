package com.pmei.shared.exception;

/**
 * Exception thrown when trying to create a resource
 * that already exists.
 */
public class ResourceAlreadyExistsException  extends RuntimeException{

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
