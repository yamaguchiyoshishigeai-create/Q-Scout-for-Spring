package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ControllerToRepositoryDirectAccessRule extends AbstractTextRule {
    private static final Pattern REPOSITORY_FIELD = Pattern.compile("(\\w+Repository)\\s+(\\w+)\\s*[;=]");

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

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (String variable : repositoryVariables) {
                if (line.contains(variable + ".") && !line.contains("class ")) {
                    violations.add(violation(file, i + 1, "Controller accesses repository directly via " + variable + ".", Severity.HIGH));
                }
            }
        }
        return violations;
    }

    private boolean looksLikeController(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains("@Controller") || line.contains("@RestController"));
    }
}
