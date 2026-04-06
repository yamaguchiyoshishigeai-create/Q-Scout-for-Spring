package com.qscout.spring.infrastructure;

import com.qscout.spring.application.ScoreCalculator;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.domain.Severity;
import org.springframework.stereotype.Component;

@Component
public class DefaultScoreCalculator implements ScoreCalculator {
    @Override
    public ScoreSummary calculate(AnalysisResult analysisResult) {
        int high = count(analysisResult, Severity.HIGH);
        int medium = count(analysisResult, Severity.MEDIUM);
        int low = count(analysisResult, Severity.LOW);
        int deduction = high * 10 + medium * 5 + low * 2;
        int finalScore = Math.max(0, 100 - deduction);
        return new ScoreSummary(100, finalScore, high, medium, low, analysisResult.allViolations().size());
    }

    private int count(AnalysisResult result, Severity severity) {
        return (int) result.allViolations().stream()
                .filter(violation -> violation.severity() == severity)
                .count();
    }
}
