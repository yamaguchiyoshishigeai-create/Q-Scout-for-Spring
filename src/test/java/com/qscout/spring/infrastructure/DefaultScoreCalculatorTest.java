package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultScoreCalculatorTest {
    private final DefaultScoreCalculator calculator = new DefaultScoreCalculator();
    private final ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());

    @Test
    void returnsPerfectScoreWhenThereAreNoViolations() {
        var summary = calculator.calculate(new AnalysisResult(context, List.of(), List.of()));

        assertThat(summary.finalScore()).isEqualTo(100);
        assertThat(summary.totalViolations()).isZero();
    }

    @Test
    void calculatesWeightedDeductionAndCounts() {
        var result = new AnalysisResult(context, List.of(), List.of(
                violation(Severity.HIGH),
                violation(Severity.MEDIUM),
                violation(Severity.MEDIUM),
                violation(Severity.LOW)
        ));

        var summary = calculator.calculate(result);

        assertThat(summary.finalScore()).isEqualTo(78);
        assertThat(summary.highCount()).isEqualTo(1);
        assertThat(summary.mediumCount()).isEqualTo(2);
        assertThat(summary.lowCount()).isEqualTo(1);
        assertThat(summary.totalViolations()).isEqualTo(4);
    }

    @Test
    void neverDropsBelowZero() {
        var violations = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> violation(Severity.HIGH))
                .toList();

        var summary = calculator.calculate(new AnalysisResult(context, List.of(), violations));

        assertThat(summary.finalScore()).isZero();
    }

    private Violation violation(Severity severity) {
        return new Violation("R", "Rule", severity, Path.of("File.java"), 1, "message", "snippet");
    }
}
