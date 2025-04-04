package com.deb.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice // Intercept exceptions globally
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    // --- Handlers for specific custom exceptions ---
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AliasGenerationException.class)
    public ResponseEntity<Object> handleAliasGenerationException(AliasGenerationException ex, WebRequest request) {
        log.error("Alias generation failed: {}", ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    // --- Override default handler for @Valid validation errors ---
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status instanceof HttpStatus ? ((HttpStatus)status).getReasonPhrase() : "Validation Error"); // Get standard reason phrase

        // Get field-specific errors
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing) // Handle potential duplicate field errors if needed
                );
        body.put("message", "Validation failed. Check field errors."); // General message
        body.put("fieldErrors", fieldErrors); // Detailed field errors
        body.put("path", request.getDescription(false).replace("uri=", ""));

        log.warn("Validation failed for request path {}: {}", body.get("path"), fieldErrors);
        // Use the status determined by Spring (usually 400 Bad Request)
        return new ResponseEntity<>(body, headers, status);
    }

    // --- Handle generic exceptions (catch-all) - map to 500 Internal Server Error ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        // Log stack trace for unexpected errors for debugging
        log.error("An unexpected error occurred processing request [{}]: {}", request.getDescription(false), ex.getMessage(), ex);
        String message = "An unexpected internal error occurred. Please try again later or contact support.";
        return buildErrorResponse(ex, message, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    // --- Helper methods to build consistent error response body ---

    /**
     * Builds a standardized error response entity using the exception's message.
     */
    private ResponseEntity<Object> buildErrorResponse(Exception ex, HttpStatus status, WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), status, request);
    }

    /**
     * Builds a standardized error response entity with a custom message.
     */
    private ResponseEntity<Object> buildErrorResponse(Exception ex, String message, HttpStatus status, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase()); // e.g., "Not Found", "Bad Request"
        body.put("message", message); // Specific error message
        body.put("path", request.getDescription(false).replace("uri=", "")); // The requested path

        // Optionally add exception class name for non-production environments
        // if (isDevelopmentEnvironment()) { // Implement this check based on active profile etc.
        //    body.put("exception", ex.getClass().getName());
        // }

        return new ResponseEntity<>(body, status);
    }
}