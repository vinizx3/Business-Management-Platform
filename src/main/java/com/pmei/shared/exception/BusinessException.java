package com.pmei.shared.exception;

/**
 * Builds standardized API error response.
 */
public class BusinessException extends RuntimeException{

    public BusinessException(String message){
        super(message);
    }
}
