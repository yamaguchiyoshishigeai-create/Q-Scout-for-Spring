package com.qscout.spring.domain;

import java.nio.file.Path;

/**
 * 共通解析で生成した成果物ファイルの配置先をまとめる DTO である。
 *
 * <p>人間向けレポートと AI 用 Markdown の両成果物を、CLI / Web 共通で受け渡す文脈で利用する。</p>
 *
 * @param humanReportPath 人間向け Markdown レポートの出力パス
 * @param aiReportPath AI 用 Markdown 成果物の出力パス
 */
public record ReportArtifact(
        Path humanReportPath,
        Path aiReportPath
) {
}
