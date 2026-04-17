package com.qscout.spring.web.dto;

public record WebAnalysisResponse(
        String requestId,
        String originalFileName,
        String executedAt,
        int finalScore,
        int totalViolations,
        int highCount,
        int mediumCount,
        int lowCount,
        DownloadLinkView humanDownloadLink,
        DownloadLinkView aiDownloadLink,
        String humanPreviewUrl,
        String aiPreviewUrl,
        String message,
        String autoExcludedMessage,
        boolean timedOut,
        boolean completed
) {
}
