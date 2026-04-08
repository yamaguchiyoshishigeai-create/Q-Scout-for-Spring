package com.qscout.spring.infrastructure;

import com.qscout.spring.application.ReportGenerator;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.util.MarkdownWriter;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;

@Component
public class MarkdownReportGenerator implements ReportGenerator {
    private final MessageSource messageSource;

    public MarkdownReportGenerator() {
        this(defaultMessageSource());
    }

    public MarkdownReportGenerator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Path generate(AnalysisResult analysisResult, ScoreSummary scoreSummary, Path outputDirectory) {
        return generate(analysisResult, scoreSummary, outputDirectory, Locale.JAPANESE);
    }

    @Override
    public Path generate(AnalysisResult analysisResult, ScoreSummary scoreSummary, Path outputDirectory, Locale locale) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(message("report.title", locale)).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- ").append(message("report.target.label", locale)).append(": ")
                .append(analysisResult.projectContext().projectRootPath()).append(System.lineSeparator());
        builder.append("- ").append(message("report.overallScore.label", locale)).append(": ")
                .append(scoreSummary.finalScore()).append("/100").append(System.lineSeparator());
        builder.append("- ").append(message("report.severityCounts.label", locale)).append(": ")
                .append(severityLabel("HIGH", locale)).append("=").append(scoreSummary.highCount())
                .append(", ").append(severityLabel("MEDIUM", locale)).append("=").append(scoreSummary.mediumCount())
                .append(", ").append(severityLabel("LOW", locale)).append("=").append(scoreSummary.lowCount()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## ").append(message("report.section.summary", locale)).append(System.lineSeparator()).append(System.lineSeparator());
        for (RuleResult ruleResult : analysisResult.ruleResults()) {
            builder.append("- ").append(ruleName(ruleResult.ruleId(), ruleResult.ruleName(), locale)).append(": ")
                    .append(ruleResult.violations().size()).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("## ").append(message("report.section.violations", locale)).append(System.lineSeparator()).append(System.lineSeparator());

        if (analysisResult.allViolations().isEmpty()) {
            builder.append(message("report.noViolations", locale)).append(System.lineSeparator());
        }

        for (Violation violation : analysisResult.allViolations()) {
            builder.append("### ").append(ruleName(violation.ruleId(), violation.ruleName(), locale)).append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("- ").append(message("report.violation.severity", locale)).append(": ")
                    .append(severityLabel(violation.severity().name(), locale)).append(System.lineSeparator());
            builder.append("- ").append(message("report.violation.file", locale)).append(": ")
                    .append(violation.filePath()).append(System.lineSeparator());
            builder.append("- ").append(message("report.violation.line", locale)).append(": ")
                    .append(violation.lineNumber()).append(System.lineSeparator());
            builder.append("- ").append(message("report.violation.message", locale)).append(": ")
                    .append(violation.message()).append(System.lineSeparator());
            if (!violation.codeSnippet().isBlank()) {
                builder.append(System.lineSeparator()).append("```java").append(System.lineSeparator());
                builder.append(violation.codeSnippet()).append(System.lineSeparator());
                builder.append("```").append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
        }

        builder.append("## ").append(message("report.section.hints", locale)).append(System.lineSeparator()).append(System.lineSeparator());
        if (analysisResult.allViolations().isEmpty()) {
            builder.append("- ").append(message("report.improvementHints.none", locale)).append(System.lineSeparator());
            builder.append("- ").append(message("report.successMessage", locale)).append(System.lineSeparator());
        } else {
            builder.append("- ").append(message("report.hint.constructorInjection", locale)).append(System.lineSeparator());
            builder.append("- ").append(message("report.hint.transactionBoundary", locale)).append(System.lineSeparator());
            builder.append("- ").append(message("report.hint.serviceMediation", locale)).append(System.lineSeparator());
            builder.append("- ").append(message("report.hint.exceptionHandling", locale)).append(System.lineSeparator());
            builder.append("- ").append(message("report.hint.testCoverage", locale)).append(System.lineSeparator());
        }

        return MarkdownWriter.write(outputDirectory, "qscout-report.md", builder.toString());
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    private String severityLabel(String severity, Locale locale) {
        return messageSource.getMessage("severity." + severity, null, severity, locale);
    }

    private String ruleName(String ruleId, String fallback, Locale locale) {
        return messageSource.getMessage("report.rule." + ruleId + ".name", null, fallback, locale);
    }

    private static MessageSource defaultMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
