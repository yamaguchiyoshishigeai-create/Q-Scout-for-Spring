package com.qscout.spring.web.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClientIpResolver {
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_CLIENT_IP = "unknown";

    private final TrustedProxyPolicy trustedProxyPolicy;

    public ClientIpResolver(TrustedProxyPolicy trustedProxyPolicy) {
        this.trustedProxyPolicy = trustedProxyPolicy;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = fallbackRemoteAddr(request.getRemoteAddr());
        if (!trustedProxyPolicy.shouldTrustForwardedHeaders(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedClientIp = extractForwardedClientIp(request.getHeader(FORWARDED_FOR_HEADER));
        return forwardedClientIp != null ? forwardedClientIp : remoteAddr;
    }

    static String extractForwardedClientIp(String forwardedFor) {
        if (!StringUtils.hasText(forwardedFor)) {
            return null;
        }
        String candidate = forwardedFor.split(",", 2)[0].trim();
        return TrustedProxyPolicy.normalizeIpLiteral(candidate);
    }

    private static String fallbackRemoteAddr(String remoteAddr) {
        return StringUtils.hasText(remoteAddr) ? remoteAddr.trim() : UNKNOWN_CLIENT_IP;
    }
}
