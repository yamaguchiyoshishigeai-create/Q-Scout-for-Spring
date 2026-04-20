package com.qscout.spring.domain;

import java.nio.file.Path;

/**
 * CLI 実行完了後に返す要約情報をまとめる DTO である。
 *
 * <p>スコア概要と生成成果物パスを、CLI 入口から呼び出し元へ返す文脈で利用する。</p>
 *
 * @param finalScore 解析後の最終スコア
 * @param totalViolations 検出違反総数
 * @param humanReportPath 人間向け Markdown レポートの出力パス
 * @param aiReportPath AI 用 Markdown 成果物の出力パス
 */
public record ExecutionSummary(
        int finalScore,
        int totalViolations,
        Path humanReportPath,
        Path aiReportPath
) {
}
