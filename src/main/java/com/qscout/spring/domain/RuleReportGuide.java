package com.qscout.spring.domain;

public record RuleReportGuide(
        String severityReadingHint,
        String firstCheckPoint,
        String quickImprovementDirection,
        String nuanceNote
) {
}
