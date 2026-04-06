package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ScoreSummary;

public interface ScoreCalculator {
    ScoreSummary calculate(AnalysisResult analysisResult);
}
