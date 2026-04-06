package com.qscout.spring;

import com.qscout.spring.cli.ArgumentParser;
import com.qscout.spring.cli.CliApplication;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ExecutionSummary;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.infrastructure.DefaultProjectScanner;
import com.qscout.spring.infrastructure.DefaultRuleEngine;
import com.qscout.spring.infrastructure.DefaultScoreCalculator;
import com.qscout.spring.infrastructure.MarkdownReportGenerator;
import com.qscout.spring.rule.ControllerToRepositoryDirectAccessRule;
import com.qscout.spring.rule.ExceptionSwallowingRule;
import com.qscout.spring.rule.FieldInjectionRule;
import com.qscout.spring.rule.MissingTestRule;
import com.qscout.spring.rule.PackageDependencyViolationRule;
import com.qscout.spring.rule.Rule;
import com.qscout.spring.rule.TransactionalMisuseRule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QScoutCliTest {
    private final Path sampleProject = Path.of("src/test/resources/sample-project").toAbsolutePath().normalize();

    @Test
    void parsesArguments() {
        ArgumentParser parser = new ArgumentParser();
        AnalysisRequest request = parser.parse(new String[]{"target/project", "target/out"});

        assertEquals(Path.of("target/project"), request.projectRootPath());
        assertEquals(Path.of("target/out"), request.outputDirectory());
    }

    @Test
    void failsWhenPomIsMissing() throws IOException {
        Path tempProject = Files.createTempDirectory("qscout-no-pom");
        Files.createDirectories(tempProject.resolve("src/main/java"));

        DefaultProjectScanner scanner = new DefaultProjectScanner();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> scanner.scan(new AnalysisRequest(tempProject, tempProject.resolve("out")))
        );

        assertTrue(exception.getMessage().contains("pom.xml"));
    }

    @Test
    void detectsFieldInjectionAndExceptionSwallowing() {
        AnalysisResult analysisResult = analyzeSample();

        assertTrue(analysisResult.allViolations().stream().anyMatch(v -> v.ruleId().equals("R002")));
        assertTrue(analysisResult.allViolations().stream().anyMatch(v -> v.ruleId().equals("R004")));
    }

    @Test
    void calculatesScore() {
        DefaultScoreCalculator calculator = new DefaultScoreCalculator();
        ScoreSummary summary = calculator.calculate(analyzeSample());

        assertEquals(46, summary.finalScore());
        assertEquals(9, summary.totalViolations());
        assertEquals(3, summary.highCount());
        assertEquals(4, summary.mediumCount());
        assertEquals(2, summary.lowCount());
    }

    @Test
    void generatesReportFiles() throws IOException {
        CliApplication application = new CliApplication();
        Path output = Files.createTempDirectory("qscout-report-out");

        ExecutionSummary summary = application.run(new String[]{sampleProject.toString(), output.toString()});

        assertTrue(Files.exists(summary.humanReportPath()));
        assertTrue(Files.exists(summary.aiReportPath()));
        assertTrue(Files.readString(summary.humanReportPath()).contains("Q-Scout Report"));
        assertTrue(Files.readString(summary.aiReportPath()).contains("Project Analysis Input"));
    }

    @Test
    void sampleProjectProducesUsefulViolations() {
        AnalysisResult analysisResult = analyzeSample();

        assertTrue(analysisResult.allViolations().stream().anyMatch(v -> v.ruleId().equals("R001")));
        assertTrue(analysisResult.allViolations().stream().anyMatch(v -> v.ruleId().equals("R005")));
        assertTrue(analysisResult.allViolations().stream().anyMatch(v -> !v.codeSnippet().isBlank()));
        assertFalse(analysisResult.ruleResults().isEmpty());
    }

    private AnalysisResult analyzeSample() {
        DefaultProjectScanner scanner = new DefaultProjectScanner();
        ProjectContext context = scanner.scan(new AnalysisRequest(sampleProject, sampleProject.resolve("out")));
        List<Rule> rules = List.of(
                new ControllerToRepositoryDirectAccessRule(),
                new FieldInjectionRule(),
                new TransactionalMisuseRule(),
                new ExceptionSwallowingRule(),
                new MissingTestRule(),
                new PackageDependencyViolationRule()
        );
        DefaultRuleEngine engine = new DefaultRuleEngine(rules);
        AnalysisResult result = engine.analyze(context);
        assertEquals(6, result.ruleResults().size());
        assertTrue(result.ruleResults().stream().map(RuleResult::violations).flatMap(List::stream).count() > 0);
        return result;
    }
}

