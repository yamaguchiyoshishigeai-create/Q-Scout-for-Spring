package com.qscout.spring.web.service;

import com.qscout.spring.web.dto.RateLimitDecision;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRequestRateLimiterTest {
    @Test
    void allowsRequestsUntilWindowLimit() {
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(new HashMap<>(),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC));

        RateLimitDecision first = limiter.evaluate("203.0.113.10");
        RateLimitDecision second = limiter.evaluate("203.0.113.10");

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
    }

    @Test
    void rejectsRequestsOverLimit() {
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(new HashMap<>(),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC));

        for (int i = 0; i < InMemoryRequestRateLimiter.MAX_REQUESTS_PER_WINDOW; i++) {
            assertThat(limiter.evaluate("203.0.113.11").allowed()).isTrue();
        }

        RateLimitDecision denied = limiter.evaluate("203.0.113.11");

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isPositive();
    }

    @Test
    void keepsDifferentIpsIndependent() {
        InMemoryRequestRateLimiter limiter = new InMemoryRequestRateLimiter(new HashMap<>(),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC));

        for (int i = 0; i < InMemoryRequestRateLimiter.MAX_REQUESTS_PER_WINDOW; i++) {
            limiter.evaluate("203.0.113.12");
        }

        assertThat(limiter.evaluate("203.0.113.12").allowed()).isFalse();
        assertThat(limiter.evaluate("203.0.113.13").allowed()).isTrue();
    }
}
