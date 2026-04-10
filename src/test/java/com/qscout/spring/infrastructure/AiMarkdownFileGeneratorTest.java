package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class AiMarkdownFileGeneratorTest {
    private final AiMarkdownFileGenerator generator = new AiMarkdownFileGenerator(new InMemoryRuleExplanationCatalog());

    @TempDir
    Path tempDir;

    @Test
    void generatesAiMarkdownWithExpectedEnglishStructure() throws IOException {
        Path path = generator.generate(analysisResultWithViolation(), tempDir);
        String content = Files.readString(path);

        assertThat(path.getFileName().toString()).isEqualTo("qscout-ai-input.md");
        assertThat(content).contains("# Project Analysis Input");
        assertThat(content).contains("## Project Summary");
        assertThat(content).contains("## Detected Issues");
        assertThat(content).contains("### Issue 1");
        assertThat(content).contains("Rule meaning: An exception may be caught and then hidden without logging, wrapping, or rethrowing.");
        assertThat(content).contains("Nuance: This rule flags caught exceptions that disappear without logging, wrapping, or rethrowing.");
        assertThat(content).contains("## Instructions");
    }

    @Test
    void keepsAiMarkdownInEnglishEvenWhenJapaneseLocaleIsSelected() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            Path path = generator.generate(analysisResultWithViolation(), tempDir);
            String content = Files.readString(path);

            assertThat(content).contains("# Project Analysis Input");
            assertThat(content).contains("## Instructions");
            assertThat(content).contains("Rule meaning:");
            assertThat(content).doesNotContain("Q-Scout 診断レポート");
            assertThat(content).doesNotContain("改善ヒント");
            assertThat(content).doesNotContain("違反は検出されませんでした。");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
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
        assertThat(content).doesNotContain("違反は検出されませんでした。");
    }

    @Test
    void keepsAiMarkdownCompactByAddingOnlyShortHints() throws IOException {
        Path path = generator.generate(analysisResultWithViolation(), tempDir);
        String content = Files.readString(path);

        assertThat(content).contains("- Rule meaning:");
        assertThat(content).contains("- Nuance:");
        assertThat(content).doesNotContain("Why Q-Scout cares");
        assertThat(content).doesNotContain("Conditionally Acceptable Cases");
    }

    private AnalysisResult analysisResultWithViolation() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(Path.of("Main.java")), List.of(Path.of("MainTest.java")));
        Violation violation = new Violation("R004", "Exception Swallowing", Severity.HIGH, Path.of("SampleService.java"), 20, "message", "20: return null;");
        RuleResult ruleResult = new RuleResult("R004", "Exception Swallowing", List.of(violation));
        return new AnalysisResult(context, List.of(ruleResult), List.of(violation));
    }
}
