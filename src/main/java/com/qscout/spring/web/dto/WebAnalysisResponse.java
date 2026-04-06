package com.qscout.spring.web.dto;

public record WebAnalysisResponse(
        String requestId,
        int finalScore,
        int totalViolations,
        int highCount,
        int mediumCount,
        int lowCount,
        DownloadLinkView humanDownloadLink,
        DownloadLinkView aiDownloadLink,
        String message,
        boolean timedOut,
        boolean completed
) {
}
