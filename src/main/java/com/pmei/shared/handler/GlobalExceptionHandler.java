package com.pmei.shared.handler;

import com.pmei.shared.dto.ApiErrorResponse;
import com.pmei.shared.exception.BusinessException;
import com.pmei.shared.exception.EntityNotFoundException;
import com.pmei.shared.exception.ResourceAlreadyExistsException;
import com.pmei.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 *
 * Responsible for intercepting exceptions thrown by controllers
 * and returning standardized API error responses.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final Clock clock;

    /**
     * Handles business rule violations.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {

        log.warn("Business exception: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Business Rule Violation",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Handles entity not found exceptions (JPA or custom).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {

        log.warn("Entity not found: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Handles validation errors from @Valid.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining(", "));

        log.warn("Validation error on fields: {}", message);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "Invalid fields: " + message,
                request.getRequestURI()
        );
    }

    /**
     * Handles validation errors from @Valid.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unexpected error", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Unexpected error occurred",
                request.getRequestURI()
        );
    }

    /**
     * Handles custom resource not found exceptions.
     *
     * Triggered when a specific resource (e.g., Company, Product, Contract)
     * cannot be found in the system.
     *
     * Returns HTTP 404 status.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Handles cases where a resource already exists.
     *
     * Triggered when attempting to create a resource that violates
     * uniqueness constraints (e.g., email, document).
     *
     * Returns HTTP 409 (Conflict).
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(
            ResourceAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource already exists: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.CONFLICT,
                "Resource Already Exists",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Builds standardized API error response.
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {

        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(clock),
                status.value(),
                error,
                message,
                path
        );

        return ResponseEntity.status(status).body(response);
    }
}
