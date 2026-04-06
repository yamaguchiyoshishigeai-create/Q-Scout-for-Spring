package com.qscout.spring.domain;

import java.nio.file.Path;

public record Violation(
        String ruleId,
        String ruleName,
        Severity severity,
        Path filePath,
        Integer lineNumber,
        String message,
        String codeSnippet
) {
}
