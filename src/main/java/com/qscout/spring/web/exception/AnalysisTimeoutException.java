package com.qscout.spring.web.exception;

public class AnalysisTimeoutException extends RuntimeException implements LocalizedUserMessageException {
    private final String messageKey;
    private final Object[] messageArgs;

    public AnalysisTimeoutException(String message, Throwable cause) {
        super(message, cause);
        this.messageKey = null;
        this.messageArgs = new Object[0];
    }

    public AnalysisTimeoutException(String messageKey, String defaultMessage, Throwable cause, Object... messageArgs) {
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
