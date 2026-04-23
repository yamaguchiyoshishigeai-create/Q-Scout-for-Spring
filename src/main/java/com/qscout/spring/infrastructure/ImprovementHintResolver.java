package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.RuleResult;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ImprovementHintResolver {
    private static final List<String> NO_VIOLATION_HINT_KEYS = List.of(
            "report.hint.none.1",
            "report.hint.none.2"
    );

    private static final Map<String, List<String>> RULE_HINT_KEYS = Map.of(
            "R001", List.of("report.hint.rule.R001.1"),
            "R002", List.of("report.hint.rule.R002.1"),
            "R003", List.of("report.hint.rule.R003.1"),
            "R004", List.of("report.hint.rule.R004.1"),
            "R005", List.of("report.hint.rule.R005.1"),
            "R006", List.of("report.hint.rule.R006.1")
    );

    public List<String> resolveMessageKeys(AnalysisResult analysisResult) {
        if (analysisResult.allViolations().isEmpty()) {
            return NO_VIOLATION_HINT_KEYS;
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (RuleResult ruleResult : analysisResult.ruleResults()) {
            if (ruleResult.violations().isEmpty()) {
                continue;
            }
            keys.addAll(RULE_HINT_KEYS.getOrDefault(ruleResult.ruleId(), List.of()));
        }
        return List.copyOf(keys);
    }
}
