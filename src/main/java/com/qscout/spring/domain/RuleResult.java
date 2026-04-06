package com.qscout.spring.domain;

import java.util.List;

public record RuleResult(
        String ruleId,
        String ruleName,
        List<Violation> violations
) {
}
