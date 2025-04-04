package com.deb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Use 503 Service Unavailable or 500 Internal Server Error
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AliasGenerationException extends RuntimeException {
    public AliasGenerationException(String message) {
        super(message);
    }
}