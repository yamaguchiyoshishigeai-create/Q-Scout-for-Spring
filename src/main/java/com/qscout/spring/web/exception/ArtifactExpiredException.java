package com.qscout.spring.web.exception;

public class ArtifactExpiredException extends RuntimeException {
    public ArtifactExpiredException(String message) {
        super(message);
    }
}
