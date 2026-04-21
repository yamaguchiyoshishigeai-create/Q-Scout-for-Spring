package com.qscout.spring.web.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class ClientIpResolver {
    private static final Logger logger = LoggerFactory.getLogger(ClientIpResolver.class);
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_CLIENT_IP = "unknown";
    private static final String MASK_NONE = "none";
    private static final String MASK_INVALID = "invalid";

    private final TrustedProxyPolicy trustedProxyPolicy;
    private final boolean observationLogEnabled;

    public ClientIpResolver(
            TrustedProxyPolicy trustedProxyPolicy,
            @Value("${qscout.client-ip.observation-log-enabled:false}") boolean observationLogEnabled
    ) {
        this.trustedProxyPolicy = trustedProxyPolicy;
        this.observationLogEnabled = observationLogEnabled;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = fallbackRemoteAddr(request.getRemoteAddr());
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        boolean trustedProxyMatched = trustedProxyPolicy.shouldTrustForwardedHeaders(remoteAddr);
        boolean forwardedHeaderPresent = forwardedFor != null;
        String forwardedClientIp = extractForwardedClientIp(forwardedFor);
        boolean useForwardedClientIp = trustedProxyMatched && forwardedClientIp != null;
        String resolvedClientIp = useForwardedClientIp ? forwardedClientIp : remoteAddr;

        logObservation(
                remoteAddr,
                trustedProxyMatched,
                forwardedHeaderPresent,
                forwardedClientIp != null,
                useForwardedClientIp ? "forwarded" : "remote",
                resolvedClientIp
        );
        return resolvedClientIp;
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

    static String maskClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return MASK_NONE;
        }
        String candidate = clientIp.trim();
        String normalizedIp = TrustedProxyPolicy.normalizeIpLiteral(candidate);
        if (normalizedIp == null) {
            return MASK_INVALID;
        }
        if (normalizedIp.contains(".")) {
            return maskIpv4(normalizedIp);
        }
        if (normalizedIp.contains(":")) {
            return maskIpv6(normalizedIp);
        }
        return MASK_INVALID;
    }

    private void logObservation(
            String remoteAddr,
            boolean trustedProxyMatched,
            boolean forwardedHeaderPresent,
            boolean forwardedClientIpExtracted,
            String resolvedSource,
            String resolvedClientIp
    ) {
        if (!observationLogEnabled) {
            return;
        }
        logger.info(
                "client-ip observation remoteAddr={} trustedProxyMatched={} forwardedHeaderPresent={} "
                        + "forwardedClientIpExtracted={} resolvedSource={} resolvedClientIpMasked={}",
                remoteAddr,
                trustedProxyMatched,
                forwardedHeaderPresent,
                forwardedClientIpExtracted,
                resolvedSource,
                maskClientIp(resolvedClientIp)
        );
    }

    private static String maskIpv4(String ipv4Address) {
        int lastDot = ipv4Address.lastIndexOf('.');
        if (lastDot < 0) {
            return MASK_INVALID;
        }
        return ipv4Address.substring(0, lastDot + 1) + "xxx";
    }

    private static String maskIpv6(String ipv6Address) {
        try {
            InetAddress address = InetAddress.getByName(ipv6Address);
            if (!(address instanceof Inet6Address)) {
                return MASK_INVALID;
            }
            byte[] raw = address.getAddress();
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 8; index += 2) {
                if (builder.length() > 0) {
                    builder.append(':');
                }
                builder.append(String.format("%02x%02x", raw[index] & 0xff, raw[index + 1] & 0xff));
            }
            return builder + ":xxxx:xxxx:xxxx:xxxx";
        } catch (UnknownHostException exception) {
            return MASK_INVALID;
        }
    }
}
