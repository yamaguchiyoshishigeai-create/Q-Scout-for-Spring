package com.qscout.spring.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAccessTokenServiceTest {
    @Test
    void acceptsValidToken() {
        RequestAccessTokenService service = new RequestAccessTokenService(
                "secret",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC)
        );

        String url = service.createSignedUrl("/download/req-1/human", "req-1", "human");
        String token = query(url, "token");
        long expires = Long.parseLong(query(url, "expires"));

        assertThat(service.isValid("req-1", "human", expires, token)).isTrue();
    }

    @Test
    void rejectsTamperedRequestId() {
        RequestAccessTokenService service = new RequestAccessTokenService(
                "secret",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC)
        );

        String url = service.createSignedUrl("/download/req-1/human", "req-1", "human");

        assertThat(service.isValid("req-2", "human", Long.parseLong(query(url, "expires")), query(url, "token"))).isFalse();
    }

    @Test
    void rejectsTamperedFileKey() {
        RequestAccessTokenService service = new RequestAccessTokenService(
                "secret",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC)
        );

        String url = service.createSignedUrl("/download/req-1/human", "req-1", "human");

        assertThat(service.isValid("req-1", "ai", Long.parseLong(query(url, "expires")), query(url, "token"))).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        Instant issuedAt = Instant.parse("2026-04-17T00:00:00Z");
        RequestAccessTokenService issuingService = new RequestAccessTokenService(
                "secret",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(issuedAt, ZoneOffset.UTC)
        );
        RequestAccessTokenService validatingService = new RequestAccessTokenService(
                "secret",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(issuedAt.plus(Duration.ofMinutes(16)), ZoneOffset.UTC)
        );

        String url = issuingService.createSignedUrl("/download/req-1/human", "req-1", "human");

        assertThat(validatingService.isValid("req-1", "human", Long.parseLong(query(url, "expires")), query(url, "token"))).isFalse();
    }

    @Test
    void rejectsSecretMismatch() {
        RequestAccessTokenService issuingService = new RequestAccessTokenService(
                "secret-a",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC)
        );
        RequestAccessTokenService validatingService = new RequestAccessTokenService(
                "secret-b",
                false,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-04-17T00:00:00Z"), ZoneOffset.UTC)
        );

        String url = issuingService.createSignedUrl("/download/req-1/human", "req-1", "human");

        assertThat(validatingService.isValid("req-1", "human", Long.parseLong(query(url, "expires")), query(url, "token"))).isFalse();
    }

    private String query(String url, String key) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst(key);
    }
}
