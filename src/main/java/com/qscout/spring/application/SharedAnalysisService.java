package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ReportArtifact;
import com.qscout.spring.domain.ScoreSummary;
import org.springframework.stereotype.Service;

@Service
public class SharedAnalysisService {
    private final ProjectScanner projectScanner;
    private final RuleEngine ruleEngine;
    private final ScoreCalculator scoreCalculator;
    private final ReportGenerator reportGenerator;
    private final AiMarkdownGenerator aiMarkdownGenerator;

    public SharedAnalysisService(
            ProjectScanner projectScanner,
            RuleEngine ruleEngine,
            ScoreCalculator scoreCalculator,
            ReportGenerator reportGenerator,
            AiMarkdownGenerator aiMarkdownGenerator
    ) {
        this.projectScanner = projectScanner;
        this.ruleEngine = ruleEngine;
        this.scoreCalculator = scoreCalculator;
        this.reportGenerator = reportGenerator;
        this.aiMarkdownGenerator = aiMarkdownGenerator;
    }

    public SharedAnalysisResult execute(AnalysisRequest request) {
        AnalysisResult analysisResult = ruleEngine.analyze(projectScanner.scan(request));
        ScoreSummary scoreSummary = scoreCalculator.calculate(analysisResult);
        ReportArtifact reportArtifact = new ReportArtifact(
                reportGenerator.generate(analysisResult, scoreSummary, request.outputDirectory()),
                aiMarkdownGenerator.generate(analysisResult, request.outputDirectory())
        );
        return new SharedAnalysisResult(analysisResult, scoreSummary, reportArtifact);
    }

    public record SharedAnalysisResult(
            AnalysisResult analysisResult,
            ScoreSummary scoreSummary,
            ReportArtifact reportArtifact
    ) {
    }
}
