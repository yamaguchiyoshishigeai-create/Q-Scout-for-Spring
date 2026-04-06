package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ScoreSummary;

import java.nio.file.Path;

public interface ReportGenerator {
    Path generate(
            AnalysisResult analysisResult,
            ScoreSummary scoreSummary,
            Path outputDirectory
    );
}
