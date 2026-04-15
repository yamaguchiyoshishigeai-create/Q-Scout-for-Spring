package com.qscout.spring.web.dto;

public record SummaryDisplayView(
        WebAnalysisResponse response,
        boolean sample,
        String artifactHref
) {
}
