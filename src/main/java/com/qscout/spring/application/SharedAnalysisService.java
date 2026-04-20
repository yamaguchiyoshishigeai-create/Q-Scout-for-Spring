package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ReportArtifact;
import com.qscout.spring.domain.ScoreSummary;
import org.springframework.stereotype.Service;

/**
 * CLI / Web 共通で利用される解析実行の中核サービスである。
 *
 * <p>{@link AnalysisRequest} を受け取り、プロジェクト走査、ルール評価、スコア算出、
 * 成果物生成を一連の流れとして実行する。</p>
 *
 * <p>Web 専用の画面応答や HTTP 入出力は持たず、入口層から共通解析へ接続する責務に留める。</p>
 */
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

    /**
     * 指定された解析要求を共通解析パイプラインへ流し込み、成果物を含む結果を返す。
     *
     * @param request 解析対象プロジェクトと成果物出力先を含む解析要求
     * @return 解析結果、スコア集計、生成成果物をまとめた結果
     */
    public SharedAnalysisResult execute(AnalysisRequest request) {
        AnalysisResult analysisResult = ruleEngine.analyze(projectScanner.scan(request));
        ScoreSummary scoreSummary = scoreCalculator.calculate(analysisResult);
        ReportArtifact reportArtifact = new ReportArtifact(
                reportGenerator.generate(analysisResult, scoreSummary, request.outputDirectory()),
                aiMarkdownGenerator.generate(analysisResult, request.outputDirectory())
        );
        return new SharedAnalysisResult(analysisResult, scoreSummary, reportArtifact);
    }

    /**
     * 共通解析パイプラインの主要な出力をまとめて保持する値オブジェクトである。
     *
     * @param analysisResult ルール評価結果
     * @param scoreSummary スコア集計結果
     * @param reportArtifact 生成した成果物の参照
     */
    public record SharedAnalysisResult(
            AnalysisResult analysisResult,
            ScoreSummary scoreSummary,
            ReportArtifact reportArtifact
    ) {
    }
}
