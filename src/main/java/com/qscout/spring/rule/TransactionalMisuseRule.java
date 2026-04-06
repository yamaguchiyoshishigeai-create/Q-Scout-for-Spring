package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransactionalMisuseRule extends AbstractTextRule {
    @Override
    public String ruleId() {
        return "R003";
    }

    @Override
    public String ruleName() {
        return "Transactional Misuse";
    }

    @Override
    protected List<Violation> evaluateFile(ProjectContext projectContext, Path file, List<String> lines) {
        List<Violation> violations = new ArrayList<>();
        boolean isController = lines.stream().anyMatch(line -> line.contains("@Controller") || line.contains("@RestController"));
        boolean isService = lines.stream().anyMatch(line -> line.contains("@Service")) || file.getFileName().toString().endsWith("Service.java");
        boolean hasTransactional = lines.stream().anyMatch(line -> line.contains("@Transactional"));

        if (isController && hasTransactional) {
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("@Transactional")) {
                    violations.add(violation(file, i + 1, "Controller should usually not manage transactions directly.", Severity.HIGH));
                }
            }
        }

        if (isService && !hasTransactional) {
            violations.add(violation(file, 1, "Service-like class may be missing transactional boundary.", Severity.MEDIUM));
        }

        return violations;
    }
}
