package com.qscout.spring.domain;

import java.nio.file.Path;

/**
 * 共通解析へ渡す入力条件をまとめる要求 DTO である。
 *
 * <p>CLI と Web の両入口から、解析対象プロジェクトと成果物出力先を受け渡す文脈で利用する。</p>
 *
 * @param projectRootPath 解析対象プロジェクトのルートディレクトリ
 * @param outputDirectory 生成成果物の出力先ディレクトリ
 */
public record AnalysisRequest(
        Path projectRootPath,
        Path outputDirectory
) {
}
