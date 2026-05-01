package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.ReportArtifact;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedAnalysisServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void orchestratesAnalysisPipelineAndWritesLocalizedHumanReports() throws IOException {
        ProjectScanner projectScanner = mock(ProjectScanner.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        ScoreCalculator scoreCalculator = mock(ScoreCalculator.class);
        ReportGenerator reportGenerator = mock(ReportGenerator.class);
        AiMarkdownGenerator aiMarkdownGenerator = mock(AiMarkdownGenerator.class);
        SharedAnalysisService service = new SharedAnalysisService(projectScanner, ruleEngine, scoreCalculator, reportGenerator, aiMarkdownGenerator);

        Path outputDirectory = tempDir.resolve("out");
        Files.createDirectories(outputDirectory);
        AnalysisRequest request = new AnalysisRequest(Path.of("project"), outputDirectory);
        ProjectContext projectContext = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());
        AnalysisResult analysisResult = new AnalysisResult(projectContext, List.<RuleResult>of(), List.of());
        ScoreSummary scoreSummary = new ScoreSummary(100, 90, 1, 0, 0, 1);
        Path humanPath = outputDirectory.resolve(SharedAnalysisService.HUMAN_REPORT_FILE_NAME);
        Path aiPath = outputDirectory.resolve("qscout-ai-input.md");

        when(projectScanner.scan(request)).thenReturn(projectContext);
        when(ruleEngine.analyze(projectContext)).thenReturn(analysisResult);
        when(scoreCalculator.calculate(analysisResult)).thenReturn(scoreSummary);
        when(reportGenerator.generate(analysisResult, scoreSummary, request.outputDirectory()))
                .thenAnswer(invocation -> {
                    Files.writeString(humanPath, "human report");
                    return humanPath;
                });
        when(aiMarkdownGenerator.generate(analysisResult, request.outputDirectory())).thenReturn(aiPath);

        SharedAnalysisService.SharedAnalysisResult result = service.execute(request);

        verify(projectScanner).scan(request);
        verify(ruleEngine).analyze(projectContext);
        verify(scoreCalculator).calculate(analysisResult);
        assertThat(Files.readString(outputDirectory.resolve(SharedAnalysisService.HUMAN_REPORT_JA_FILE_NAME))).isEqualTo("human report");
        assertThat(Files.readString(outputDirectory.resolve(SharedAnalysisService.HUMAN_REPORT_EN_FILE_NAME))).isEqualTo("human report");
        assertThat(result.scoreSummary()).isEqualTo(scoreSummary);
        assertThat(result.reportArtifact()).isEqualTo(new ReportArtifact(humanPath, aiPath));
    }
}
