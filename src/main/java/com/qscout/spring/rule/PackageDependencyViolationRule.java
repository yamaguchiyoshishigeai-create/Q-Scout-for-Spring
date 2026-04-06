package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PackageDependencyViolationRule extends AbstractTextRule {
    @Override
    public String ruleId() {
        return "R006";
    }

    @Override
    public String ruleName() {
        return "Package Dependency Violation";
    }

    @Override
    protected List<Violation> evaluateFile(ProjectContext projectContext, Path file, List<String> lines) {
        List<Violation> violations = new ArrayList<>();
        boolean controller = file.toString().contains("\\controller\\") || file.getFileName().toString().endsWith("Controller.java");
        boolean repository = file.toString().contains("\\repository\\") || file.getFileName().toString().endsWith("Repository.java");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (controller && line.contains("import ") && line.contains(".repository.")) {
                violations.add(violation(file, i + 1, "Controller imports repository layer directly.", Severity.MEDIUM));
            }
            if (repository && line.contains("import ") && (line.contains(".service.") || line.contains(".controller."))) {
                violations.add(violation(file, i + 1, "Repository depends on upper layer package.", Severity.MEDIUM));
            }
        }
        return violations;
    }
}
