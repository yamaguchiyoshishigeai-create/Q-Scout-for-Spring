package com.qscout.spring.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TrustedProxyPolicy {
    private final boolean trustForwardedHeaders;
    private final Set<String> trustedProxies;

    @Autowired
    public TrustedProxyPolicy(
            @Value("${qscout.client-ip.trust-forwarded-headers:false}") boolean trustForwardedHeaders,
            @Value("${qscout.client-ip.trusted-proxies:}") String configuredTrustedProxies
    ) {
        this(trustForwardedHeaders, Arrays.asList(configuredTrustedProxies.split(",")));
    }

    private TrustedProxyPolicy(boolean trustForwardedHeaders, Collection<String> configuredTrustedProxies) {
        this.trustForwardedHeaders = trustForwardedHeaders;
        this.trustedProxies = configuredTrustedProxies.stream()
                .map(TrustedProxyPolicy::normalizeIpLiteral)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean shouldTrustForwardedHeaders(String remoteAddr) {
        if (!trustForwardedHeaders) {
            return false;
        }
        String normalizedRemoteAddr = normalizeIpLiteral(remoteAddr);
        return normalizedRemoteAddr != null && trustedProxies.contains(normalizedRemoteAddr);
    }

    public static TrustedProxyPolicy forTesting(boolean trustForwardedHeaders, String... trustedProxies) {
        return new TrustedProxyPolicy(trustForwardedHeaders, Arrays.asList(trustedProxies));
    }

    static String normalizeIpLiteral(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String candidate = value.trim();
        if (isIpv4Literal(candidate)) {
            return candidate;
        }
        if (!looksLikeIpv6Literal(candidate)) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(candidate);
            if (address instanceof Inet6Address) {
                return address.getHostAddress();
            }
            return null;
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static boolean isIpv4Literal(String candidate) {
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (!StringUtils.hasText(octet) || octet.length() > 3) {
                return false;
            }
            for (int index = 0; index < octet.length(); index++) {
                if (!Character.isDigit(octet.charAt(index))) {
                    return false;
                }
            }
            int value = Integer.parseInt(octet);
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeIpv6Literal(String candidate) {
        if (!candidate.contains(":")) {
            return false;
        }
        for (int index = 0; index < candidate.length(); index++) {
            char current = candidate.charAt(index);
            if (Character.isDigit(current)
                    || (current >= 'a' && current <= 'f')
                    || (current >= 'A' && current <= 'F')
                    || current == ':'
                    || current == '.'
                    || current == '%') {
                continue;
            }
            return false;
        }
        return true;
    }
}
