package com.qscout.spring.domain;

public record ScoreSummary(
        int initialScore,
        int finalScore,
        int highCount,
        int mediumCount,
        int lowCount,
        int totalViolations
) {
}
