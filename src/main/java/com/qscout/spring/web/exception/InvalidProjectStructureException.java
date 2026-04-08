package com.qscout.spring.web.exception;

public class InvalidProjectStructureException extends RuntimeException implements LocalizedUserMessageException {
    private final String messageKey;
    private final Object[] messageArgs;

    public InvalidProjectStructureException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = new Object[0];
    }

    public InvalidProjectStructureException(String messageKey, String defaultMessage, Object... messageArgs) {
        super(defaultMessage);
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
