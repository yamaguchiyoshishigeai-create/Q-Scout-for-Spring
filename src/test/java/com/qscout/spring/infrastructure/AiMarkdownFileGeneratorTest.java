package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiMarkdownFileGeneratorTest {
    private final AiMarkdownFileGenerator generator = new AiMarkdownFileGenerator();

    @TempDir
    Path tempDir;

    @Test
    void generatesAiMarkdownWithExpectedStructure() throws IOException {
        Path path = generator.generate(analysisResultWithViolation(), tempDir);
        String content = Files.readString(path);

        assertThat(path.getFileName().toString()).isEqualTo("qscout-ai-input.md");
        assertThat(content).contains("# Project Analysis Input");
        assertThat(content).contains("## Project Summary");
        assertThat(content).contains("## Detected Issues");
        assertThat(content).contains("### Issue 1");
        assertThat(content).contains("## Instructions");
    }

    @Test
    void generatesAiMarkdownWhenThereAreNoViolations() throws IOException {
        AnalysisResult empty = new AnalysisResult(
                new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of()),
                List.of(),
                List.of()
        );
        Path path = generator.generate(empty, tempDir);
        String content = Files.readString(path);

        assertThat(content).contains("No issues detected.");
        assertThat(content).contains("## Instructions");
    }

    private AnalysisResult analysisResultWithViolation() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(Path.of("Main.java")), List.of(Path.of("MainTest.java")));
        Violation violation = new Violation("R004", "Exception Swallowing", Severity.HIGH, Path.of("SampleService.java"), 20, "message", "20: return null;");
        return new AnalysisResult(context, List.of(), List.of(violation));
    }
}
