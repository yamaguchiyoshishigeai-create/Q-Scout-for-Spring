package com.qscout.spring.infrastructure;

import com.qscout.spring.application.RuleEngine;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.rule.Rule;

import java.util.ArrayList;
import java.util.List;

public class DefaultRuleEngine implements RuleEngine {
    private final List<Rule> rules;

    public DefaultRuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public AnalysisResult analyze(ProjectContext projectContext) {
        List<RuleResult> results = rules.stream()
                .map(rule -> rule.evaluate(projectContext))
                .toList();
        List<Violation> allViolations = new ArrayList<>();
        results.forEach(result -> allViolations.addAll(result.violations()));
        return new AnalysisResult(projectContext, results, List.copyOf(allViolations));
    }
}
