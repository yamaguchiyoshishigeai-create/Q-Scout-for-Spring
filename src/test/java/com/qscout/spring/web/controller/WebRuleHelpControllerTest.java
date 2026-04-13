package com.qscout.spring.web.controller;

import com.qscout.spring.application.RuleExplanationCatalog;
import com.qscout.spring.domain.RuleExplanation;
import com.qscout.spring.domain.RuleReportGuide;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WebRuleHelpControllerTest {
    @Test
    void showsRuleHelpPageWhenSlugExists() throws Exception {
        RuleExplanationCatalog catalog = mock(RuleExplanationCatalog.class);
        RuleExplanation explanation = new RuleExplanation(
                "R001",
                "Controller To Repository Direct Access",
                "Summary",
                "Why",
                "Typical",
                "Improve",
                "Allowance",
                "Interpretation",
                "Cares",
                "rule-explanations/controller-to-repository-direct-access",
                "Short guidance",
                "AI guidance",
                new RuleReportGuide("Severity", "First", "Direction", "Nuance")
        );
        when(catalog.findAll(any())).thenReturn(Map.of("R001", explanation));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebRuleHelpController(catalog)).build();

        mockMvc.perform(get("/help/rules/controller-to-repository-direct-access"))
                .andExpect(status().isOk())
                .andExpect(view().name("rule-help"))
                .andExpect(model().attributeExists("rule"));
    }

    @Test
    void returnsNotFoundForUnknownRuleSlug() throws Exception {
        RuleExplanationCatalog catalog = mock(RuleExplanationCatalog.class);
        when(catalog.findAll(any())).thenReturn(Map.of());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebRuleHelpController(catalog)).build();

        mockMvc.perform(get("/help/rules/unknown-rule"))
                .andExpect(status().isNotFound());
    }
}
