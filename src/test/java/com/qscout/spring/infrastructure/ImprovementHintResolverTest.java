package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImprovementHintResolverTest {
    private final ImprovementHintResolver resolver = new ImprovementHintResolver();

    @Test
    void returnsNoViolationHintsWhenThereAreNoViolations() {
        AnalysisResult result = analysisResult(List.of(
                ruleResult("R001", List.of()),
                ruleResult("R002", List.of())
        ));

        assertEquals(
                List.of("report.hint.none.1", "report.hint.none.2"),
                resolver.resolveMessageKeys(result)
        );
    }

    @Test
    void returnsOnlyTheHintForTheViolatedRule() {
        AnalysisResult result = analysisResult(List.of(
                ruleResult("R001", List.of(violation("R001"))),
                ruleResult("R002", List.of()),
                ruleResult("R003", List.of())
        ));

        assertEquals(
                List.of("report.hint.rule.R001.1"),
                resolver.resolveMessageKeys(result)
        );
    }

    @Test
    void returnsHintsForMultipleViolatedRulesInRuleResultOrder() {
        AnalysisResult result = analysisResult(List.of(
                ruleResult("R002", List.of(violation("R002"))),
                ruleResult("R001", List.of(violation("R001"))),
                ruleResult("R004", List.of(violation("R004")))
        ));

        assertEquals(
                List.of(
                        "report.hint.rule.R002.1",
                        "report.hint.rule.R001.1",
                        "report.hint.rule.R004.1"
                ),
                resolver.resolveMessageKeys(result)
        );
    }

    @Test
    void suppressesDuplicateHintsWhenSameRuleAppearsMoreThanOnce() {
        AnalysisResult result = analysisResult(List.of(
                ruleResult("R001", List.of(violation("R001"))),
                ruleResult("R001", List.of(violation("R001"), violation("R001"))),
                ruleResult("R002", List.of(violation("R002")))
        ));

        assertEquals(
                List.of(
                        "report.hint.rule.R001.1",
                        "report.hint.rule.R002.1"
                ),
                resolver.resolveMessageKeys(result)
        );
    }

    @Test
    void ignoresUnknownRuleIdsWithoutFallingBackToGenericHints() {
        AnalysisResult result = analysisResult(List.of(
                ruleResult("UNKNOWN", List.of(violation("UNKNOWN"))),
                ruleResult("R005", List.of(violation("R005")))
        ));

        assertEquals(
                List.of("report.hint.rule.R005.1"),
                resolver.resolveMessageKeys(result)
        );
    }

    private AnalysisResult analysisResult(List<RuleResult> ruleResults) {
        List<Violation> allViolations = ruleResults.stream()
                .flatMap(ruleResult -> ruleResult.violations().stream())
                .toList();
        return new AnalysisResult(
                new ProjectContext(
                        Path.of("/project"),
                        Path.of("/project/pom.xml"),
                        List.of(Path.of("/project/src/main/java/Sample.java")),
                        List.of(Path.of("/project/src/test/java/SampleTest.java"))
                ),
                ruleResults,
                allViolations
        );
    }

    private RuleResult ruleResult(String ruleId, List<Violation> violations) {
        return new RuleResult(ruleId, ruleId + " name", violations);
    }

    private Violation violation(String ruleId) {
        return new Violation(
                ruleId,
                ruleId + " name",
                Severity.MEDIUM,
                Path.of("src/main/java/Sample.java"),
                12,
                ruleId + " violation",
                "class Sample {}"
        );
    }
}
