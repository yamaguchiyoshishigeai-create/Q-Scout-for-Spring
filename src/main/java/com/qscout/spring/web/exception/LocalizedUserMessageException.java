package com.qscout.spring.web.exception;

public interface LocalizedUserMessageException {
    String getMessageKey();

    Object[] getMessageArgs();
}
