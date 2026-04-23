package com.qscout.spring.web.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreBandClassResolverTest {
    @Test
    void resolvesScoreBandClassesAtThresholds() {
        assertThat(ScoreBandClassResolver.fromScore(100)).isEqualTo(ScoreBandClassResolver.HIGH);
        assertThat(ScoreBandClassResolver.fromScore(80)).isEqualTo(ScoreBandClassResolver.HIGH);
        assertThat(ScoreBandClassResolver.fromScore(79)).isEqualTo(ScoreBandClassResolver.MEDIUM);
        assertThat(ScoreBandClassResolver.fromScore(40)).isEqualTo(ScoreBandClassResolver.MEDIUM);
        assertThat(ScoreBandClassResolver.fromScore(39)).isEqualTo(ScoreBandClassResolver.LOW);
        assertThat(ScoreBandClassResolver.fromScore(0)).isEqualTo(ScoreBandClassResolver.LOW);
    }
}
