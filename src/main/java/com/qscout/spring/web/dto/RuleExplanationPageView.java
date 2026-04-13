package com.qscout.spring.web.dto;

public record RuleExplanationPageView(
        String slug,
        String ruleId,
        String displayName,
        String shortSummary,
        String whyItMatters,
        String interpretationHint,
        String conditionalAllowance,
        String whyQScoutCares,
        String severityReadingHint,
        String firstCheckPoint,
        String quickImprovementDirection,
        String nuanceNote
) {
}
