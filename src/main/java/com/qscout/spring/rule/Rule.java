package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;

public interface Rule {
    String ruleId();

    String ruleName();

    RuleResult evaluate(ProjectContext projectContext);
}
