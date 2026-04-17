package com.qscout.spring.web.service;

import com.qscout.spring.web.dto.RateLimitDecision;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryRequestRateLimiter implements RequestRateLimiter {
    static final int MAX_REQUESTS_PER_WINDOW = 5;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> requestBuckets;
    private final Clock clock;

    public InMemoryRequestRateLimiter() {
        this(new ConcurrentHashMap<>(), Clock.systemUTC());
    }

    InMemoryRequestRateLimiter(Map<String, Deque<Instant>> requestBuckets, Clock clock) {
        this.requestBuckets = requestBuckets;
        this.clock = clock;
    }

    @Override
    public RateLimitDecision evaluate(String clientKey) {
        Instant now = clock.instant();
        Instant threshold = now.minus(WINDOW);
        Deque<Instant> timestamps = requestBuckets.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
                Instant retryAt = timestamps.peekFirst().plus(WINDOW);
                long retryAfterSeconds = Math.max(1, Duration.between(now, retryAt).getSeconds());
                return RateLimitDecision.deny(retryAfterSeconds);
            }
            timestamps.addLast(now);
            return RateLimitDecision.allow(MAX_REQUESTS_PER_WINDOW - timestamps.size());
        }
    }
}
