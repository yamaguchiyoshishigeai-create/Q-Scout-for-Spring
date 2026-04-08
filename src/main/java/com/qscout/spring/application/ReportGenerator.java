package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ScoreSummary;

import java.nio.file.Path;
import java.util.Locale;

public interface ReportGenerator {
    Path generate(
            AnalysisResult analysisResult,
            ScoreSummary scoreSummary,
            Path outputDirectory
    );

    default Path generate(
            AnalysisResult analysisResult,
            ScoreSummary scoreSummary,
            Path outputDirectory,
            Locale locale
    ) {
        return generate(analysisResult, scoreSummary, outputDirectory);
    }
}
