package com.qscout.spring.web.dto;

public record RateLimitDecision(
        boolean allowed,
        int remainingRequests,
        long retryAfterSeconds
) {
    public static RateLimitDecision allow(int remainingRequests) {
        return new RateLimitDecision(true, remainingRequests, 0);
    }

    public static RateLimitDecision deny(long retryAfterSeconds) {
        return new RateLimitDecision(false, 0, retryAfterSeconds);
    }
}
