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
        String normalizedPath = file.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalizedPath.contains("/rule/")) {
            return violations;
        }

        boolean isController = lines.stream().anyMatch(line -> line.contains("@Controller") || line.contains("@RestController"));
        boolean isService = lines.stream().anyMatch(line -> line.contains("@Service")) || normalizedPath.contains("/service/");
        boolean hasTransactional = lines.stream().anyMatch(line -> line.contains("@Transactional"));
        boolean usesRepository = lines.stream().anyMatch(line ->
                line.contains("Repository")
                        || line.contains("EntityManager")
                        || line.contains("@PersistenceContext")
        );

        if (isController && hasTransactional) {
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("@Transactional")) {
                    violations.add(violation(file, i + 1, "Controller should usually not manage transactions directly.", Severity.HIGH));
                }
            }
        }

        if (isService && usesRepository && !hasTransactional) {
            violations.add(violation(file, 1, "Service-like class may be missing transactional boundary.", Severity.MEDIUM));
        }

        return violations;
    }
}
