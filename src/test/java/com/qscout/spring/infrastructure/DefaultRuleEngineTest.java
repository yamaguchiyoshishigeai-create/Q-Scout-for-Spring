package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.rule.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRuleEngineTest {
    private final ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());

    @Test
    void aggregatesRuleResultsAndViolations() {
        Violation violation1 = new Violation("R1", "Rule One", Severity.HIGH, Path.of("A.java"), 1, "a", "snippet");
        Violation violation2 = new Violation("R2", "Rule Two", Severity.LOW, Path.of("B.java"), 2, "b", "snippet");
        DefaultRuleEngine engine = new DefaultRuleEngine(List.of(
                fixedRule("R1", "Rule One", List.of(violation1)),
                fixedRule("R2", "Rule Two", List.of(violation2))
        ));

        AnalysisResult result = engine.analyze(context);

        assertThat(result.ruleResults()).hasSize(2);
        assertThat(result.allViolations()).containsExactly(violation1, violation2);
    }

    @Test
    void returnsEmptyViolationsWhenRulesFindNothing() {
        DefaultRuleEngine engine = new DefaultRuleEngine(List.of(
                fixedRule("R1", "Rule One", List.of()),
                fixedRule("R2", "Rule Two", List.of())
        ));

        AnalysisResult result = engine.analyze(context);

        assertThat(result.ruleResults()).hasSize(2);
        assertThat(result.allViolations()).isEmpty();
    }

    private Rule fixedRule(String id, String name, List<Violation> violations) {
        return new Rule() {
            @Override
            public String ruleId() {
                return id;
            }

            @Override
            public String ruleName() {
                return name;
            }

            @Override
            public RuleResult evaluate(ProjectContext projectContext) {
                return new RuleResult(id, name, violations);
            }
        };
    }
}
