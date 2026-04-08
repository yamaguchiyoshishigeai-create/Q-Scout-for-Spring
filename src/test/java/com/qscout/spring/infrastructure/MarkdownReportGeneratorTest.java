package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportGeneratorTest {
    private final MarkdownReportGenerator generator = new MarkdownReportGenerator();

    @TempDir
    Path tempDir;

    @Test
    void generatesMarkdownReportWithMainSections() throws IOException {
        Path reportPath = generator.generate(analysisResultWithViolation(), new ScoreSummary(100, 88, 0, 1, 1, 2), tempDir);
        String content = Files.readString(reportPath);

        assertThat(reportPath.getFileName().toString()).isEqualTo("qscout-report.md");
        assertThat(content).contains("# Q-Scout Report");
        assertThat(content).contains("Overall Score: 88/100");
        assertThat(content).contains("Severity Counts");
        assertThat(content).contains("## Rule Summary");
        assertThat(content).contains("## Violations");
        assertThat(content).contains("Controller To Repository Direct Access");
    }

    @Test
    void generatesReportEvenWhenThereAreNoViolations() throws IOException {
        AnalysisResult empty = new AnalysisResult(
                new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of()),
                List.of(),
                List.of()
        );
        Path reportPath = generator.generate(empty, new ScoreSummary(100, 100, 0, 0, 0, 0), tempDir);
        String content = Files.readString(reportPath);

        assertThat(content).contains("No violations detected.");
        assertThat(content).contains("## Improvement Hints");
    }

    private AnalysisResult analysisResultWithViolation() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());
        Violation violation = new Violation("R001", "Controller To Repository Direct Access", Severity.MEDIUM, Path.of("SampleController.java"), 12, "message", "12: sampleRepository.findAll();");
        RuleResult ruleResult = new RuleResult("R001", "Controller To Repository Direct Access", List.of(violation));
        return new AnalysisResult(context, List.of(ruleResult), List.of(violation));
    }
}
