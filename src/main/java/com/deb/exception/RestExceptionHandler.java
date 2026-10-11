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

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<Object> handleLinkNotFound(LinkNotFoundException ex, WebRequest request) {
        log.warn("Link not found: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ResponseEntity<Object> handleLinkExpired(LinkExpiredException ex, WebRequest request) {
        log.warn("Link expired: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.GONE, request);
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<Object> handleSlugAlreadyExists(SlugAlreadyExistsException ex, WebRequest request) {
        log.warn("Slug conflict: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Database constraint conflict: {}", ex.getMessage());
        return buildErrorResponse("Slug or resource already in use. Please choose a different slug.", HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(InvalidDestinationUrlException.class)
    public ResponseEntity<Object> handleInvalidUrl(InvalidDestinationUrlException ex, WebRequest request) {
        log.warn("Invalid URL: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ReservedSlugException.class)
    public ResponseEntity<Object> handleReservedSlug(ReservedSlugException ex, WebRequest request) {
        log.warn("Reserved slug error: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AliasGenerationException.class)
    public ResponseEntity<Object> handleAliasGeneration(AliasGenerationException ex, WebRequest request) {
        log.error("Alias generation failed: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status instanceof HttpStatus ? ((HttpStatus) status).getReasonPhrase() : "Validation Error");

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing));
        body.put("message", "Validation failed for request parameters");
        body.put("fieldErrors", fieldErrors);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled error processing request [{}]: {}", request.getDescription(false), ex.getMessage(), ex);
        return buildErrorResponse("An unexpected internal error occurred.", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, status);
    }
}