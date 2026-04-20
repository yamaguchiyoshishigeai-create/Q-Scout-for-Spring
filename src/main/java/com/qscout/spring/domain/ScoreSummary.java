package com.qscout.spring.domain;

/**
 * 解析結果から算出したスコア集計情報をまとめる DTO である。
 *
 * <p>CLI と Web の双方で、最終スコアと重大度別件数を要約表示する文脈で利用する。</p>
 *
 * @param initialScore 減点前の基準スコア
 * @param finalScore 違反内容を反映した最終スコア
 * @param highCount 高重大度違反の件数
 * @param mediumCount 中重大度違反の件数
 * @param lowCount 低重大度違反の件数
 * @param totalViolations 検出違反総数
 */
public record ScoreSummary(
        int initialScore,
        int finalScore,
        int highCount,
        int mediumCount,
        int lowCount,
        int totalViolations
) {
}
