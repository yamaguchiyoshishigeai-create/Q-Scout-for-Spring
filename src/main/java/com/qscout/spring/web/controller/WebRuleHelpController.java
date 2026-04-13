package com.qscout.spring.web.controller;

import com.qscout.spring.application.RuleExplanationCatalog;
import com.qscout.spring.domain.RuleExplanation;
import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.RuleExplanationPageView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class WebRuleHelpController {
    private final RuleExplanationCatalog ruleExplanationCatalog;

    public WebRuleHelpController(RuleExplanationCatalog ruleExplanationCatalog) {
        this.ruleExplanationCatalog = ruleExplanationCatalog;
    }

    @GetMapping("/help/rules/{slug}")
    public String showRuleHelp(@PathVariable String slug, Model model) {
        Locale locale = MessageSources.resolveLocale();
        RuleExplanation explanation = ruleExplanationCatalog.findAll(locale).values().stream()
                .filter(candidate -> slug.equals(extractSlug(candidate.detailPageKey())))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Rule explanation not found: " + slug));

        model.addAttribute("rule", new RuleExplanationPageView(
                slug,
                explanation.ruleId(),
                explanation.displayName(),
                explanation.shortSummary(),
                explanation.whyItMatters(),
                explanation.interpretationHint(),
                explanation.conditionalAllowance(),
                explanation.whyQScoutCares(),
                explanation.reportGuide().severityReadingHint(),
                explanation.reportGuide().firstCheckPoint(),
                explanation.reportGuide().quickImprovementDirection(),
                explanation.reportGuide().nuanceNote()
        ));
        return "rule-help";
    }

    private String extractSlug(String detailPageKey) {
        if (detailPageKey == null || detailPageKey.isBlank()) {
            return "";
        }
        int slashIndex = detailPageKey.lastIndexOf('/');
        return slashIndex >= 0 ? detailPageKey.substring(slashIndex + 1) : detailPageKey;
    }
}
