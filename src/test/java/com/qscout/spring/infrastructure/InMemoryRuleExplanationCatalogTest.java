package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.RuleExplanation;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRuleExplanationCatalogTest {
    private final InMemoryRuleExplanationCatalog catalog = new InMemoryRuleExplanationCatalog();

    @Test
    void returnsAllSixRuleExplanations() {
        Map<String, RuleExplanation> explanations = catalog.findAll(Locale.ENGLISH);

        assertThat(explanations).hasSize(6);
        assertThat(explanations).containsKeys("R001", "R002", "R003", "R004", "R005", "R006");
    }

    @Test
    void providesDetailedExplanationForControllerToRepositoryRule() {
        RuleExplanation explanation = catalog.findByRuleId("R001", Locale.JAPANESE);

        assertThat(explanation.displayName()).isEqualTo("Controller から Repository への直接アクセス");
        assertThat(explanation.conditionalAllowance()).contains("単純な read-only 参照");
        assertThat(explanation.interpretationHint()).contains("絶対悪ではなく");
        assertThat(explanation.whyQScoutCares()).contains("早めに捉える");
        assertThat(explanation.reportShortGuidance()).contains("Service へ寄せるべき");
        assertThat(explanation.aiShortGuidance()).contains("controllers are directly handling persistence concerns");
        assertThat(explanation.detailPageKey()).isEqualTo("rule-explanations/controller-to-repository-direct-access");
        assertThat(explanation.reportGuide().firstCheckPoint()).contains("複数 Repository");
    }
}
