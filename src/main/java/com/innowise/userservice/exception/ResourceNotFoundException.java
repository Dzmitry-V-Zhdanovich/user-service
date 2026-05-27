package com.innowise.userservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s с идентификатором '%s' не найден", resourceName, id));
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object value) {
        super(String.format("%s с %s '%s' не найден", resourceName, fieldName, value));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
