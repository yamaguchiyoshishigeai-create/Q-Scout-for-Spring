package com.qscout.spring.web.exception;

public class InvalidUploadException extends RuntimeException implements LocalizedUserMessageException {
    private final String messageKey;
    private final Object[] messageArgs;

    public InvalidUploadException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = new Object[0];
    }

    public InvalidUploadException(String message, Throwable cause) {
        super(message, cause);
        this.messageKey = null;
        this.messageArgs = new Object[0];
    }

    public InvalidUploadException(String messageKey, String defaultMessage, Object... messageArgs) {
        super(defaultMessage);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs.clone();
    }

    public InvalidUploadException(String messageKey, String defaultMessage, Throwable cause, Object... messageArgs) {
        super(defaultMessage, cause);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs.clone();
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }
}
