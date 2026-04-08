package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FieldInjectionRule extends AbstractTextRule {
    @Override
    public String ruleId() {
        return "R002";
    }

    @Override
    public String ruleName() {
        return "Field Injection";
    }

    @Override
    protected List<Violation> evaluateFile(ProjectContext projectContext, Path file, List<String> lines) {
        List<Violation> violations = new ArrayList<>();
        String normalizedPath = file.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalizedPath.contains("/rule/")) {
            return violations;
        }
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("@Autowired")) {
                violations.add(violation(file, i + 1, "Field injection detected. Prefer constructor injection.", Severity.MEDIUM));
            }
        }
        return violations;
    }
}
