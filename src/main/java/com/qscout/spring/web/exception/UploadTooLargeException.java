package com.qscout.spring.web.exception;

public class UploadTooLargeException extends InvalidUploadException {
    public UploadTooLargeException(String message) {
        super(message);
    }
}