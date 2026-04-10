package com.qscout.spring.infrastructure;

import com.qscout.spring.application.ReportGenerator;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.util.MarkdownWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;

@Component
public class MarkdownReportGenerator implements ReportGenerator {
    private final MessageSource messageSource;

    public MarkdownReportGenerator() {
        this(MessageSources.create());
    }

    public MarkdownReportGenerator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Path generate(AnalysisResult analysisResult, ScoreSummary scoreSummary, Path outputDirectory) {
        Locale locale = MessageSources.resolveLocale();
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(message(locale, "report.title")).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- ").append(message(locale, "report.target")).append(": ").append(analysisResult.projectContext().projectRootPath()).append(System.lineSeparator());
        builder.append("- ").append(message(locale, "report.overallScore")).append(": ").append(scoreSummary.finalScore()).append("/100").append(System.lineSeparator());
        builder.append("- ").append(message(locale, "report.severityCounts")).append(": HIGH=").append(scoreSummary.highCount())
                .append(", MEDIUM=").append(scoreSummary.mediumCount())
                .append(", LOW=").append(scoreSummary.lowCount()).append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("## ").append(message(locale, "report.section.summary")).append(System.lineSeparator()).append(System.lineSeparator());
        for (RuleResult ruleResult : analysisResult.ruleResults()) {
            builder.append("- ").append(ruleName(locale, ruleResult.ruleId(), ruleResult.ruleName())).append(": ")
                    .append(ruleResult.violations().size()).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("## ").append(message(locale, "report.section.violations")).append(System.lineSeparator()).append(System.lineSeparator());

        if (analysisResult.allViolations().isEmpty()) {
            builder.append(message(locale, "report.noViolations")).append(System.lineSeparator());
        }

        for (Violation violation : analysisResult.allViolations()) {
            builder.append("### ").append(ruleName(locale, violation.ruleId(), violation.ruleName())).append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.label.severity")).append(": ").append(violation.severity()).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.label.file")).append(": ").append(violation.filePath()).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.label.line")).append(": ").append(violation.lineNumber()).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.label.message")).append(": ").append(localizeViolationMessage(locale, violation)).append(System.lineSeparator());
            if (!violation.codeSnippet().isBlank()) {
                builder.append(System.lineSeparator()).append("```java").append(System.lineSeparator());
                builder.append(violation.codeSnippet()).append(System.lineSeparator());
                builder.append("```").append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
        }

        builder.append("## ").append(message(locale, "report.section.improvementHints")).append(System.lineSeparator()).append(System.lineSeparator());
        if (analysisResult.allViolations().isEmpty()) {
            builder.append("- ").append(message(locale, "report.hint.none.1")).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.hint.none.2")).append(System.lineSeparator());
        } else {
            builder.append("- ").append(message(locale, "report.hint.1")).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.hint.2")).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.hint.3")).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.hint.4")).append(System.lineSeparator());
            builder.append("- ").append(message(locale, "report.hint.5")).append(System.lineSeparator());
        }

        return MarkdownWriter.write(outputDirectory, "qscout-report.md", builder.toString());
    }

    private String localizeViolationMessage(Locale locale, Violation violation) {
        String key = switch (violation.ruleId()) {
            case "R001" -> switch (violation.severity()) {
                case HIGH -> "rule.R001.message.high";
                case MEDIUM -> "rule.R001.message.medium";
                case LOW -> "rule.R001.message.low";
            };
            case "R002" -> "rule.R002.message.fieldInjection";
            case "R003" -> violation.message().contains("service-like")
                    ? "rule.R003.message.serviceBoundary"
                    : "rule.R003.message.controllerTransactional";
            case "R004" -> "rule.R004.message.swallowed";
            case "R005" -> "rule.R005.message.missingTest";
            case "R006" -> violation.message().contains("Controller imports")
                    ? "rule.R006.message.controllerImportsRepository"
                    : "rule.R006.message.repositoryDependsUpper";
            default -> null;
        };
        if (key == null) {
            return violation.message();
        }
        Object[] args = "R005".equals(violation.ruleId()) ? new Object[]{extractMissingTestSubject(violation.message())} : null;
        return messageSource.getMessage(key, args, violation.message(), locale);
    }

    private String extractMissingTestSubject(String message) {
        int index = message.indexOf(" for ");
        if (index >= 0) {
            return message.substring(index + 5).replace(".", "").trim();
        }
        return message;
    }

    private String ruleName(Locale locale, String ruleId, String defaultName) {
        return messageSource.getMessage("rule." + ruleId + ".name", null, defaultName, locale);
    }

    private String message(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
