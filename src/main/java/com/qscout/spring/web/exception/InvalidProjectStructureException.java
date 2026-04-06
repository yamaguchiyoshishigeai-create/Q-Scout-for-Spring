package com.qscout.spring.web.exception;

public class InvalidProjectStructureException extends RuntimeException {
    public InvalidProjectStructureException(String message) {
        super(message);
    }
}
