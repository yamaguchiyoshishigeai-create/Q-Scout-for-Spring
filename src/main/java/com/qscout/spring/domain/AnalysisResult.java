package com.qscout.spring.domain;

import java.util.List;

/**
 * 解析対象プロジェクトに対するルール評価結果全体をまとめる DTO である。
 *
 * <p>共通解析パイプライン内で、プロジェクト文脈、ルール別結果、違反一覧を束ねて扱う。</p>
 *
 * @param projectContext 解析対象プロジェクトの文脈情報
 * @param ruleResults ルールごとの評価結果一覧
 * @param allViolations 検出された違反一覧
 */
public record AnalysisResult(
        ProjectContext projectContext,
        List<RuleResult> ruleResults,
        List<Violation> allViolations
) {
}
