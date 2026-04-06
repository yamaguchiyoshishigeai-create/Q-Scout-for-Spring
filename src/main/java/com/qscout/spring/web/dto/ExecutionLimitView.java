package com.qscout.spring.web.dto;

public record ExecutionLimitView(
        int maxUploadSizeMb,
        int maxExecutionSeconds,
        String allowedFileType
) {
}
