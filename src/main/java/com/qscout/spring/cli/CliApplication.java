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

/**
 * CLI 実行の入口として、引数解析と共通解析サービス呼び出しを仲介するアプリケーションである。
 *
 * <p>コマンドライン引数から {@link AnalysisRequest} を組み立て、解析結果を
 * {@link ExecutionSummary} として返す。</p>
 *
 * <p>Web 応答や HTTP の責務は持たず、ローカル実行経路の維持に専念する。</p>
 */
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

    /**
     * CLI 引数を解析し、共通解析を実行して出力サマリを返す。
     *
     * @param args CLI から渡された引数列
     * @return スコアと生成成果物パスを含む CLI 実行結果サマリ
     */
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
