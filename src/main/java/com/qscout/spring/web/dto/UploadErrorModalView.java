package com.qscout.spring.web.dto;

public record UploadErrorModalView(
        String title,
        String body,
        String retry
) {
}