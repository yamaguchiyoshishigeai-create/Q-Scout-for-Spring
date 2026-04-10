package com.qscout.spring.application;

import com.qscout.spring.domain.RuleExplanation;

import java.util.Locale;
import java.util.Map;

public interface RuleExplanationCatalog {
    RuleExplanation findByRuleId(String ruleId, Locale locale);

    Map<String, RuleExplanation> findAll(Locale locale);
}
