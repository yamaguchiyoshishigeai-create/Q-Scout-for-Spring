package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.i18n.MessageSources;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MissingTestRule implements Rule {
    private final MessageSource messageSource;

    public MissingTestRule() {
        this(MessageSources.create());
    }

    public MissingTestRule(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String ruleId() {
        return "R005";
    }

    @Override
    public String ruleName() {
        return "Missing Test";
    }

    @Override
    public RuleResult evaluate(ProjectContext projectContext) {
        Set<String> testNames = projectContext.testJavaFiles().stream()
                .map(path -> path.getFileName().toString().replace(".java", ""))
                .collect(Collectors.toSet());

        List<Violation> violations = new ArrayList<>();
        for (Path mainFile : projectContext.mainJavaFiles()) {
            String className = mainFile.getFileName().toString().replace(".java", "");
            if (className.equals("package-info") || className.equals("module-info")) {
                continue;
            }
            if (!shouldRequireTest(mainFile, className)) {
                continue;
            }
            if (!testNames.contains(className + "Test")) {
                violations.add(new Violation(
                        ruleId(),
                        ruleName(),
                        Severity.LOW,
                        mainFile,
                        1,
                        message("rule.R005.message.missingTest", "Test class not found for {0}.", className),
                        ""
                ));
            }
        }

        return new RuleResult(ruleId(), ruleName(), List.copyOf(violations));
    }

    private boolean shouldRequireTest(Path mainFile, String className) {
        String normalizedPath = mainFile.toString().replace('\\', '/').toLowerCase(Locale.ROOT);

        if (className.equals("Main")
                || normalizedPath.contains("/domain/")
                || normalizedPath.contains("/dto/")
                || normalizedPath.contains("/exception/")
                || normalizedPath.contains("/enum/")) {
            return false;
        }

        if (normalizedPath.contains("/web/service/") || normalizedPath.contains("/web/controller/")) {
            return true;
        }

        if (normalizedPath.contains("/application/") && className.equals("SharedAnalysisService")) {
            return true;
        }

        if (normalizedPath.contains("/infrastructure/") || normalizedPath.contains("/rule/")) {
            return isConcreteClass(mainFile);
        }

        return false;
    }

    private boolean isConcreteClass(Path file) {
        try {
            String source = Files.readString(file);
            String normalized = source.replaceAll("/\\*.*?\\*/", " ")
                    .replaceAll("//.*", " ")
                    .replaceAll("\\s+", " ");
            if (normalized.contains(" record ") || normalized.contains(" interface ") || normalized.contains(" enum ")) {
                return false;
            }
            return normalized.contains(" class ") && !normalized.contains(" abstract class ");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect source file: " + file, exception);
        }
    }

    private String message(String key, String defaultMessage, Object... args) {
        return messageSource.getMessage(key, args, defaultMessage, MessageSources.resolveLocale());
    }
}
