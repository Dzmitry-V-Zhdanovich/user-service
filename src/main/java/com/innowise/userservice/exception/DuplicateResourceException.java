package com.innowise.userservice.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object value) {
        super(String.format("%s с %s '%s' уже существует", resourceName, fieldName, value));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
