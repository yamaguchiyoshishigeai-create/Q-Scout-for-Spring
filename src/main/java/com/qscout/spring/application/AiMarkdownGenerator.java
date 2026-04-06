package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisResult;

import java.nio.file.Path;

public interface AiMarkdownGenerator {
    Path generate(
            AnalysisResult analysisResult,
            Path outputDirectory
    );
}
