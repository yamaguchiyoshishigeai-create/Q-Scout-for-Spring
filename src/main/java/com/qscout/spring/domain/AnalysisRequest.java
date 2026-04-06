package com.qscout.spring.domain;

import java.nio.file.Path;

public record AnalysisRequest(
        Path projectRootPath,
        Path outputDirectory
) {
}
