package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MissingTestRule implements Rule {
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
            if (!testNames.contains(className + "Test")) {
                violations.add(new Violation(
                        ruleId(),
                        ruleName(),
                        Severity.LOW,
                        mainFile,
                        1,
                        "No matching test class found for " + className + ".",
                        ""
                ));
            }
        }
        return new RuleResult(ruleId(), ruleName(), violations);
    }
}
