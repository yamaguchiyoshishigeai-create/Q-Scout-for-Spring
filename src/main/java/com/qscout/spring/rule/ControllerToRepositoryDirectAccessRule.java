package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ControllerToRepositoryDirectAccessRule extends AbstractTextRule {
    private static final Pattern REPOSITORY_FIELD = Pattern.compile("(\\w+Repository)\\s+(\\w+)\\s*[;=]");
    private static final Pattern METHOD_DECLARATION = Pattern.compile(
            "(public|protected|private)\\s+[\\w<>,\\[\\]\\s@?]+\\s+(\\w+)\\s*\\([^;]*\\)\\s*\\{?"
    );
    private static final List<String> WRITE_METHOD_PREFIXES = List.of("save", "delete", "flush");
    private static final List<String> READ_METHOD_PREFIXES = List.of("find", "getReferenceById", "exists", "count");
    private static final List<String> LOW_METHOD_PREFIXES = List.of("findAll", "list", "loadAll", "findPetTypes");
    private static final List<String> MUTATION_HINTS = List.of(".set", ".addPet(", ".addVisit(", " update", " create", " assign", " change");
    private static final String HIGH_MESSAGE =
            "Controller performs write or use-case-level repository access directly. Move this logic behind a service layer.";
    private static final String MEDIUM_MESSAGE =
            "Controller reads from repository directly. Prefer service mediation to preserve separation of concerns and future extensibility.";
    private static final String LOW_MESSAGE =
            "Controller performs a simple read-only repository access. This may be acceptable in limited cases, but service mediation is usually preferable.";

    @Override
    public String ruleId() {
        return "R001";
    }

    @Override
    public String ruleName() {
        return "Controller To Repository Direct Access";
    }

    @Override
    protected List<Violation> evaluateFile(ProjectContext projectContext, Path file, List<String> lines) {
        List<Violation> violations = new ArrayList<>();
        if (!looksLikeController(lines)) {
            return violations;
        }

        List<String> repositoryVariables = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = REPOSITORY_FIELD.matcher(line);
            if (matcher.find()) {
                repositoryVariables.add(matcher.group(2));
            }
        }
        if (repositoryVariables.isEmpty()) {
            return violations;
        }

        List<MethodContext> methods = extractMethods(lines);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (String variable : repositoryVariables) {
                Matcher repositoryCall = repositoryCallMatcher(variable, line);
                if (repositoryCall.find() && !line.contains("class ")) {
                    String repositoryMethod = repositoryCall.group(1);
                    MethodContext method = findEnclosingMethod(methods, i);
                    Severity severity = determineSeverity(lines, i, repositoryMethod, method);
                    violations.add(violation(file, i + 1, messageFor(severity), severity));
                }
            }
        }
        return violations;
    }

    private boolean looksLikeController(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains("@Controller") || line.contains("@RestController"));
    }

    private Matcher repositoryCallMatcher(String variable, String line) {
        return Pattern.compile("\\b" + Pattern.quote(variable) + "\\s*\\.\\s*(\\w+)\\s*\\(").matcher(line);
    }

    private Severity determineSeverity(List<String> lines, int lineIndex, String repositoryMethod, MethodContext method) {
        if (isWriteMethod(repositoryMethod) || hasUseCaseLevelMutation(lines, lineIndex, method)) {
            return Severity.HIGH;
        }
        if (isReadMethod(repositoryMethod) && isLowPriorityRead(lines, lineIndex, repositoryMethod, method)) {
            return Severity.LOW;
        }
        if (isReadMethod(repositoryMethod)) {
            return Severity.MEDIUM;
        }
        return Severity.MEDIUM;
    }

    private boolean isWriteMethod(String repositoryMethod) {
        return WRITE_METHOD_PREFIXES.stream().anyMatch(repositoryMethod::startsWith);
    }

    private boolean isReadMethod(String repositoryMethod) {
        return READ_METHOD_PREFIXES.stream().anyMatch(repositoryMethod::startsWith);
    }

    private boolean isLowPriorityRead(List<String> lines, int lineIndex, String repositoryMethod, MethodContext method) {
        if (method != null && (method.hasAnnotation("ModelAttribute") || method.name().startsWith("populate"))) {
            return true;
        }
        if (LOW_METHOD_PREFIXES.stream().anyMatch(repositoryMethod::startsWith)) {
            return true;
        }
        return hasPageableContext(lines, lineIndex, method);
    }

    private boolean hasUseCaseLevelMutation(List<String> lines, int lineIndex, MethodContext method) {
        int start = Math.max(0, method != null ? method.startLine() : lineIndex - 3);
        int end = Math.min(lines.size() - 1, method != null ? method.endLine() : lineIndex + 3);
        for (int i = start; i <= end; i++) {
            String normalized = " " + lines.get(i).toLowerCase(Locale.ROOT);
            for (String hint : MUTATION_HINTS) {
                if (normalized.contains(hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasPageableContext(List<String> lines, int lineIndex, MethodContext method) {
        int start = Math.max(0, method != null ? method.startLine() : lineIndex - 3);
        int end = Math.min(lines.size() - 1, method != null ? method.endLine() : lineIndex + 3);
        for (int i = start; i <= end; i++) {
            String normalized = lines.get(i).toLowerCase(Locale.ROOT);
            if (normalized.contains("pageable") || normalized.contains("pagerequest") || normalized.contains("page<")) {
                return true;
            }
        }
        return false;
    }

    private String messageFor(Severity severity) {
        return switch (severity) {
            case HIGH -> message("rule.R001.message.high", HIGH_MESSAGE);
            case MEDIUM -> message("rule.R001.message.medium", MEDIUM_MESSAGE);
            case LOW -> message("rule.R001.message.low", LOW_MESSAGE);
        };
    }

    private MethodContext findEnclosingMethod(List<MethodContext> methods, int lineIndex) {
        for (MethodContext method : methods) {
            if (lineIndex >= method.startLine() && lineIndex <= method.endLine()) {
                return method;
            }
        }
        return null;
    }

    private List<MethodContext> extractMethods(List<String> lines) {
        List<MethodContext> methods = new ArrayList<>();
        List<String> pendingAnnotations = new ArrayList<>();
        int braceDepth = 0;
        MethodContextBuilder currentMethod = null;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();

            if (currentMethod == null && trimmed.startsWith("@")) {
                pendingAnnotations.add(trimmed);
            }

            if (currentMethod == null) {
                Matcher matcher = METHOD_DECLARATION.matcher(trimmed);
                if (matcher.find() && !trimmed.startsWith("if ") && !trimmed.startsWith("for ") && !trimmed.startsWith("while ")) {
                    currentMethod = new MethodContextBuilder(i, matcher.group(2), pendingAnnotations, braceDepth);
                    pendingAnnotations = new ArrayList<>();
                    braceDepth += braceDelta(trimmed);
                    if (currentMethod.isComplete(braceDepth)) {
                        methods.add(currentMethod.build(i));
                        currentMethod = null;
                    }
                    continue;
                }
                if (!trimmed.isBlank() && !trimmed.startsWith("@")) {
                    pendingAnnotations.clear();
                }
            } else {
                braceDepth += braceDelta(trimmed);
                if (currentMethod.isComplete(braceDepth)) {
                    methods.add(currentMethod.build(i));
                    currentMethod = null;
                }
                continue;
            }

            braceDepth += braceDelta(trimmed);
        }

        return methods;
    }

    private int braceDelta(String line) {
        int delta = 0;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '{') {
                delta++;
            } else if (current == '}') {
                delta--;
            }
        }
        return delta;
    }

    private record MethodContext(int startLine, int endLine, String name, Set<String> annotations) {
        private boolean hasAnnotation(String annotation) {
            return annotations.stream().anyMatch(value -> value.contains("@" + annotation));
        }
    }

    private static final class MethodContextBuilder {
        private final int startLine;
        private final String name;
        private final Set<String> annotations;
        private final int baseDepth;

        private MethodContextBuilder(int startLine, String name, List<String> annotations, int baseDepth) {
            this.startLine = startLine;
            this.name = name;
            this.annotations = new HashSet<>(annotations);
            this.baseDepth = baseDepth;
        }

        private boolean isComplete(int currentDepth) {
            return currentDepth == baseDepth;
        }

        private MethodContext build(int endLine) {
            return new MethodContext(startLine, endLine, name, Set.copyOf(annotations));
        }
    }
}
