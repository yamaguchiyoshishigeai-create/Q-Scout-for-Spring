package com.qscout.spring.cli;

import com.qscout.spring.application.AiMarkdownGenerator;
import com.qscout.spring.application.ProjectScanner;
import com.qscout.spring.application.ReportGenerator;
import com.qscout.spring.application.RuleEngine;
import com.qscout.spring.application.ScoreCalculator;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ExecutionSummary;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.infrastructure.AiMarkdownFileGenerator;
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

import java.nio.file.Path;
import java.util.List;

public final class CliApplication {
    private final ArgumentParser argumentParser;
    private final ProjectScanner projectScanner;
    private final RuleEngine ruleEngine;
    private final ScoreCalculator scoreCalculator;
    private final ReportGenerator reportGenerator;
    private final AiMarkdownGenerator aiMarkdownGenerator;

    public CliApplication() {
        this(
                new ArgumentParser(),
                new DefaultProjectScanner(),
                new DefaultRuleEngine(defaultRules()),
                new DefaultScoreCalculator(),
                new MarkdownReportGenerator(),
                new AiMarkdownFileGenerator()
        );
    }

    public CliApplication(
            ArgumentParser argumentParser,
            ProjectScanner projectScanner,
            RuleEngine ruleEngine,
            ScoreCalculator scoreCalculator,
            ReportGenerator reportGenerator,
            AiMarkdownGenerator aiMarkdownGenerator
    ) {
        this.argumentParser = argumentParser;
        this.projectScanner = projectScanner;
        this.ruleEngine = ruleEngine;
        this.scoreCalculator = scoreCalculator;
        this.reportGenerator = reportGenerator;
        this.aiMarkdownGenerator = aiMarkdownGenerator;
    }

    public ExecutionSummary run(String[] args) {
        AnalysisRequest request = argumentParser.parse(args);
        ProjectContext projectContext = projectScanner.scan(request);
        AnalysisResult analysisResult = ruleEngine.analyze(projectContext);
        ScoreSummary scoreSummary = scoreCalculator.calculate(analysisResult);
        Path humanReportPath = reportGenerator.generate(analysisResult, scoreSummary, request.outputDirectory());
        Path aiReportPath = aiMarkdownGenerator.generate(analysisResult, request.outputDirectory());
        return new ExecutionSummary(
                scoreSummary.finalScore(),
                scoreSummary.totalViolations(),
                humanReportPath,
                aiReportPath
        );
    }

    private static List<Rule> defaultRules() {
        return List.of(
                new ControllerToRepositoryDirectAccessRule(),
                new FieldInjectionRule(),
                new TransactionalMisuseRule(),
                new ExceptionSwallowingRule(),
                new MissingTestRule(),
                new PackageDependencyViolationRule()
        );
    }
}
