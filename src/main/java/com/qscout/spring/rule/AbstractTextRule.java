package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.util.CodeSnippetExtractor;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractTextRule implements Rule {
    private final MessageSource messageSource;

    protected AbstractTextRule() {
        this(MessageSources.create());
    }

    protected AbstractTextRule(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public RuleResult evaluate(ProjectContext projectContext) {
        List<Violation> violations = new ArrayList<>();
        for (Path file : targetFiles(projectContext)) {
            violations.addAll(evaluateFile(projectContext, file, readLines(file)));
        }
        return new RuleResult(ruleId(), ruleName(), violations);
    }

    protected List<Path> targetFiles(ProjectContext projectContext) {
        return projectContext.mainJavaFiles();
    }

    protected abstract List<Violation> evaluateFile(ProjectContext projectContext, Path file, List<String> lines);

    protected Violation violation(Path file, int lineNumber, String message, Severity severity) {
        return new Violation(
                ruleId(),
                ruleName(),
                severity,
                file,
                lineNumber,
                message,
                CodeSnippetExtractor.extract(file, lineNumber)
        );
    }

    protected String message(String key, String defaultMessage, Object... args) {
        return messageSource.getMessage(key, args, defaultMessage, MessageSources.resolveLocale());
    }

    private List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + file, exception);
        }
    }
}
