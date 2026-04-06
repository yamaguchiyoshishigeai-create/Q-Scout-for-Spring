package com.qscout.spring.web.dto;

public record ErrorViewModel(
        String userMessage,
        String detailCode,
        boolean retryable
) {
}
