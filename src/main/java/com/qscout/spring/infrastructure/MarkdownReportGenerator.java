package com.qscout.spring.infrastructure;

import com.qscout.spring.application.ReportGenerator;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.util.MarkdownWriter;

import java.nio.file.Path;

public class MarkdownReportGenerator implements ReportGenerator {
    @Override
    public Path generate(AnalysisResult analysisResult, ScoreSummary scoreSummary, Path outputDirectory) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Q-Scout Report").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Target: ").append(analysisResult.projectContext().projectRootPath()).append(System.lineSeparator());
        builder.append("- Overall Score: ").append(scoreSummary.finalScore()).append("/100").append(System.lineSeparator());
        builder.append("- Severity Counts: HIGH=").append(scoreSummary.highCount())
                .append(", MEDIUM=").append(scoreSummary.mediumCount())
                .append(", LOW=").append(scoreSummary.lowCount()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## Rule Summary").append(System.lineSeparator()).append(System.lineSeparator());
        for (RuleResult ruleResult : analysisResult.ruleResults()) {
            builder.append("- ").append(ruleResult.ruleName()).append(": ")
                    .append(ruleResult.violations().size()).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("## Violations").append(System.lineSeparator()).append(System.lineSeparator());

        if (analysisResult.allViolations().isEmpty()) {
            builder.append("No violations detected.").append(System.lineSeparator());
        }

        for (Violation violation : analysisResult.allViolations()) {
            builder.append("### ").append(violation.ruleName()).append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("- Severity: ").append(violation.severity()).append(System.lineSeparator());
            builder.append("- File: ").append(violation.filePath()).append(System.lineSeparator());
            builder.append("- Line: ").append(violation.lineNumber()).append(System.lineSeparator());
            builder.append("- Message: ").append(violation.message()).append(System.lineSeparator());
            if (!violation.codeSnippet().isBlank()) {
                builder.append(System.lineSeparator()).append("```java").append(System.lineSeparator());
                builder.append(violation.codeSnippet()).append(System.lineSeparator());
                builder.append("```").append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
        }

        builder.append("## Improvement Hints").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Replace field injection with constructor injection.").append(System.lineSeparator());
        builder.append("- Keep transaction boundaries in the service layer.").append(System.lineSeparator());
        builder.append("- Prefer service mediation over controller-to-repository access.").append(System.lineSeparator());
        builder.append("- Log, wrap, or rethrow exceptions instead of swallowing them.").append(System.lineSeparator());
        builder.append("- Fill obvious unit/integration test gaps.").append(System.lineSeparator());

        return MarkdownWriter.write(outputDirectory, "qscout-report.md", builder.toString());
    }
}
