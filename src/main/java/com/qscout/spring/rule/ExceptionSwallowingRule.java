package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
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
                String normalized = normalize(block);
                boolean rethrows = normalized.contains("throw ");
                boolean logs = containsLogging(normalized);
                boolean exits = normalized.contains("System.exit(");
                boolean empty = isEmptyCatchBlock(block);
                boolean returnsNull = normalized.contains("return null;");
                boolean returnsEmptyString = normalized.contains("return \"\";");
                boolean cleanupOnly = containsCleanup(normalized) && !logs;
                if (rethrows || exits) {
                    continue;
                }
                if (empty || returnsNull || returnsEmptyString || cleanupOnly) {
                    violations.add(violation(file, i + 1, "Caught exception appears to be swallowed without logging or rethrow.", Severity.HIGH));
                }
            }
        }
        return violations;
    }

    private String collectBlock(List<String> lines, int startIndex) {
        StringBuilder builder = new StringBuilder();
        int braceBalance = 0;
        boolean opened = false;
        for (int i = startIndex; i < lines.size(); i++) {
            String current = lines.get(i);
            builder.append(current).append(System.lineSeparator());
            String segment = current;
            if (i == startIndex) {
                int catchIndex = current.indexOf("catch");
                segment = catchIndex >= 0 ? current.substring(catchIndex) : current;
            }
            int opens = count(segment, '{');
            int closes = count(segment, '}');
            if (opens > 0) {
                opened = true;
            }
            braceBalance += opens;
            braceBalance -= closes;
            if (opened && braceBalance <= 0) {
                break;
            }
        }
        return builder.toString();
    }

    private String normalize(String block) {
        return block.replaceAll("/\\*.*?\\*/", " ")
                .replaceAll("//.*", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsLogging(String normalized) {
        return normalized.contains("log.")
                || normalized.contains("logger.")
                || normalized.contains("printStackTrace")
                || normalized.contains("System.err");
    }

    private boolean containsCleanup(String normalized) {
        return normalized.contains("close(")
                || normalized.contains("cleanup")
                || normalized.contains("deleteIfExists(")
                || normalized.contains("rollback(")
                || normalized.contains("flush(");
    }

    private boolean isEmptyCatchBlock(String block) {
        String withoutComments = block.replaceAll("/\\*.*?\\*/", " ")
                .replaceAll("//.*", " ");
        int catchIndex = withoutComments.indexOf("catch");
        String relevant = catchIndex >= 0 ? withoutComments.substring(catchIndex) : withoutComments;
        int start = relevant.indexOf('{');
        int end = relevant.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return false;
        }
        return relevant.substring(start + 1, end).trim().isEmpty();
    }

    private int count(String value, char target) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}
