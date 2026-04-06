package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExceptionSwallowingRule extends AbstractTextRule {
    @Override
    public String ruleId() {
        return "R004";
    }

    @Override
    public String ruleName() {
        return "Exception Swallowing";
    }

    @Override
    protected List<Violation> evaluateFile(ProjectContext projectContext, Path file, List<String> lines) {
        List<Violation> violations = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("catch (")) {
                String block = collectBlock(lines, i);
                String normalized = block.replaceAll("\\s+", " ").trim();
                boolean rethrows = normalized.contains("throw ");
                boolean logs = normalized.contains("log.") || normalized.contains("logger.") || normalized.contains("printStackTrace");
                boolean empty = normalized.matches(".*catch \\(.*\\) \\{ ?\\}.*");
                boolean onlyComment = normalized.matches(".*catch \\(.*\\) \\{ ?//.*");
                if (!rethrows && !logs && (empty || onlyComment || block.lines().count() <= 3)) {
                    violations.add(violation(file, i + 1, "Caught exception appears to be swallowed without logging or rethrow.", Severity.HIGH));
                }
            }
        }
        return violations;
    }

    private String collectBlock(List<String> lines, int startIndex) {
        StringBuilder builder = new StringBuilder();
        int balance = 0;
        boolean opened = false;
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            builder.append(line).append(System.lineSeparator());
            balance += count(line, '{');
            balance -= count(line, '}');
            if (line.contains("{")) {
                opened = true;
            }
            if (opened && balance <= 0) {
                break;
            }
        }
        return builder.toString();
    }

    private int count(String line, char target) {
        int total = 0;
        for (char value : line.toCharArray()) {
            if (value == target) {
                total++;
            }
        }
        return total;
    }
}
