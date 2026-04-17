package com.qscout.spring.web.service;

import com.qscout.spring.web.dto.RateLimitDecision;

public interface RequestRateLimiter {
    RateLimitDecision evaluate(String clientKey);
}
