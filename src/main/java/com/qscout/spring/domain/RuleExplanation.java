package com.qscout.spring.domain;

public record RuleExplanation(
        String ruleId,
        String displayName,
        String shortSummary,
        String whyItMatters,
        String typicalProblems,
        String generalImprovement,
        String conditionalAllowance,
        String interpretationHint,
        String whyQScoutCares,
        String detailPageKey,
        String reportShortGuidance,
        String aiShortGuidance,
        RuleReportGuide reportGuide
) {
    public static RuleExplanation fallback(String ruleId, String displayName) {
        return new RuleExplanation(
                ruleId,
                displayName,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new RuleReportGuide("", "", "", "")
        );
    }
}
