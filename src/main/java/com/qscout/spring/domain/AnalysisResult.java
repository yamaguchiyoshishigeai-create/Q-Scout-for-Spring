package com.qscout.spring.domain;

import java.util.List;

public record AnalysisResult(
        ProjectContext projectContext,
        List<RuleResult> ruleResults,
        List<Violation> allViolations
) {
}
