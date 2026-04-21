package com.qscout.spring.web.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    @Test
    void ignoresForwardedHeaderWhenRemoteAddrIsNotTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), false);

        String resolved = resolver.resolve(request("198.51.100.10", "203.0.113.1"));

        assertThat(resolved).isEqualTo("198.51.100.10");
    }

    @Test
    void usesForwardedHeaderWhenRemoteAddrIsTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), false);

        String resolved = resolver.resolve(request("127.0.0.1", "203.0.113.1, 198.51.100.200"));

        assertThat(resolved).isEqualTo("203.0.113.1");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedHeaderIsBlank() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), false);

        String resolved = resolver.resolve(request("127.0.0.1", "   "));

        assertThat(resolved).isEqualTo("127.0.0.1");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedHeaderIsMalformed() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), false);

        String resolved = resolver.resolve(request("127.0.0.1", "not-an-ip"));

        assertThat(resolved).isEqualTo("127.0.0.1");
    }

    @Test
    void ignoresForwardedHeaderWhenForwardedTrustIsDisabled() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(false, "127.0.0.1", "::1"), false);

        String resolved = resolver.resolve(request("127.0.0.1", "203.0.113.1"));

        assertThat(resolved).isEqualTo("127.0.0.1");
    }

    @Test
    void keepsResolutionBehaviorWhenObservationLoggingIsEnabled() {
        ClientIpResolver resolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), true);

        String resolved = resolver.resolve(request("127.0.0.1", "203.0.113.1, 198.51.100.200"));

        assertThat(resolved).isEqualTo("203.0.113.1");
    }

    @Test
    void emitsMaskedObservationLogOnlyWhenEnabled() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            ClientIpResolver disabledResolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), false);
            disabledResolver.resolve(request("127.0.0.1", "203.0.113.1, 198.51.100.200"));
            assertThat(appender.list).isEmpty();

            ClientIpResolver enabledResolver = new ClientIpResolver(TrustedProxyPolicy.forTesting(true, "127.0.0.1", "::1"), true);
            enabledResolver.resolve(request("127.0.0.1", "203.0.113.1, 198.51.100.200"));

            assertThat(appender.list).hasSize(1);
            String message = appender.list.get(0).getFormattedMessage();
            assertThat(message).contains("remoteAddr=127.0.0.1");
            assertThat(message).contains("trustedProxyMatched=true");
            assertThat(message).contains("forwardedHeaderPresent=true");
            assertThat(message).contains("forwardedClientIpExtracted=true");
            assertThat(message).contains("resolvedSource=forwarded");
            assertThat(message).contains("resolvedClientIpMasked=203.0.113.xxx");
            assertThat(message).doesNotContain("203.0.113.1, 198.51.100.200");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void masksResolvedClientIpForIpv4Ipv6AndInvalidValues() {
        assertThat(ClientIpResolver.maskClientIp("203.0.113.10")).isEqualTo("203.0.113.xxx");
        assertThat(ClientIpResolver.maskClientIp("2001:db8::1234")).isEqualTo("2001:0db8:0000:0000:xxxx:xxxx:xxxx:xxxx");
        assertThat(ClientIpResolver.maskClientIp("   ")).isEqualTo("none");
        assertThat(ClientIpResolver.maskClientIp("unknown")).isEqualTo("invalid");
    }

    private MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(ClientIpResolver.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ClientIpResolver.class);
        logger.detachAppender(appender);
    }
}
