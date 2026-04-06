package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.util.CodeSnippetExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractTextRule implements Rule {

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

    private List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + file, exception);
        }
    }
}
