package com.qscout.spring.domain;

import java.nio.file.Path;

public record ExecutionSummary(
        int finalScore,
        int totalViolations,
        Path humanReportPath,
        Path aiReportPath
) {
}
