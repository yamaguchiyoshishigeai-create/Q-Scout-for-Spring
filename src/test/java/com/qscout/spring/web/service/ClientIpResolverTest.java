package com.qscout.spring.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    @Test
    void ignoresForwardedHeaderWhenRemoteAddrIsNotTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"));

        String resolved = resolver.resolve(request("198.51.100.10", "203.0.113.1"));

        assertThat(resolved).isEqualTo("198.51.100.10");
    }

    @Test
    void usesForwardedHeaderWhenRemoteAddrIsTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"));

        String resolved = resolver.resolve(request("127.0.0.1", "203.0.113.1, 198.51.100.200"));

        assertThat(resolved).isEqualTo("203.0.113.1");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedHeaderIsBlank() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"));

        String resolved = resolver.resolve(request("127.0.0.1", "   "));

        assertThat(resolved).isEqualTo("127.0.0.1");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedHeaderIsMalformed() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"));

        String resolved = resolver.resolve(request("127.0.0.1", "not-an-ip"));

        assertThat(resolved).isEqualTo("127.0.0.1");
    }

    @Test
    void ignoresForwardedHeaderWhenForwardedTrustIsDisabled() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(false, "127.0.0.1", "::1"));

        String resolved = resolver.resolve(request("127.0.0.1", "203.0.113.1"));

        assertThat(resolved).isEqualTo("127.0.0.1");
    }

    private MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
