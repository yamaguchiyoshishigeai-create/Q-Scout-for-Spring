package com.qscout.spring.domain;

import java.nio.file.Path;

public record ReportArtifact(
        Path humanReportPath,
        Path aiReportPath
) {
}
