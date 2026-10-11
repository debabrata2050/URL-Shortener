package com.deb.exception;

public class SlugAlreadyExistsException extends RuntimeException {
    public SlugAlreadyExistsException(String message) {
        super(message);
    }
}
