package com.qscout.spring.web.exception;

public class AnalysisTimeoutException extends RuntimeException {
    public AnalysisTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
