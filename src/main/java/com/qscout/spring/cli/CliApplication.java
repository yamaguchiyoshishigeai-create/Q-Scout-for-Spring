package com.qscout.spring.cli;

import com.qscout.spring.application.SharedAnalysisService;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.ExecutionSummary;
import com.qscout.spring.infrastructure.AiMarkdownFileGenerator;
import com.qscout.spring.infrastructure.DefaultProjectScanner;
import com.qscout.spring.infrastructure.DefaultRuleEngine;
import com.qscout.spring.infrastructure.DefaultScoreCalculator;
import com.qscout.spring.infrastructure.InMemoryRuleExplanationCatalog;
import com.qscout.spring.infrastructure.MarkdownReportGenerator;
import com.qscout.spring.rule.ControllerToRepositoryDirectAccessRule;
import com.qscout.spring.rule.ExceptionSwallowingRule;
import com.qscout.spring.rule.FieldInjectionRule;
import com.qscout.spring.rule.MissingTestRule;
import com.qscout.spring.rule.PackageDependencyViolationRule;
import com.qscout.spring.rule.Rule;
import com.qscout.spring.rule.TransactionalMisuseRule;

import java.util.List;

public final class CliApplication {
    private final ArgumentParser argumentParser;
    private final SharedAnalysisService sharedAnalysisService;

    public CliApplication() {
        this(new ArgumentParser(), createSharedAnalysisService());
    }

    public CliApplication(ArgumentParser argumentParser, SharedAnalysisService sharedAnalysisService) {
        this.argumentParser = argumentParser;
        this.sharedAnalysisService = sharedAnalysisService;
    }

    public ExecutionSummary run(String[] args) {
        AnalysisRequest request = argumentParser.parse(args);
        SharedAnalysisService.SharedAnalysisResult result = sharedAnalysisService.execute(request);
        return new ExecutionSummary(
                result.scoreSummary().finalScore(),
                result.scoreSummary().totalViolations(),
                result.reportArtifact().humanReportPath(),
                result.reportArtifact().aiReportPath()
        );
    }

    private static SharedAnalysisService createSharedAnalysisService() {
        InMemoryRuleExplanationCatalog ruleExplanationCatalog = new InMemoryRuleExplanationCatalog();
        return new SharedAnalysisService(
                new DefaultProjectScanner(),
                new DefaultRuleEngine(defaultRules()),
                new DefaultScoreCalculator(),
                new MarkdownReportGenerator(com.qscout.spring.i18n.MessageSources.create(), ruleExplanationCatalog),
                new AiMarkdownFileGenerator(ruleExplanationCatalog)
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
