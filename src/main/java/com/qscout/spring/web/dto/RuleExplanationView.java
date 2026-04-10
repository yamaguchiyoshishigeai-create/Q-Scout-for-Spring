package com.qscout.spring.web.dto;

public record RuleExplanationView(
        String ruleId,
        String displayName,
        String shortSummary,
        String reportShortGuidance,
        RuleDetailLinkView detailLink
) {
}
