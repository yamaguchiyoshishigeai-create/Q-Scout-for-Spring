package com.qscout.spring.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAccessTokenServiceLocalExecutionTest {
    @Test
    void detectsTargetClassesAsLocalDevelopmentExecution() {
        assertThat(RequestAccessTokenService.isLocalDevelopmentClassesPath(
                Path.of("C:/academia/src/Q-Scout-for-Spring1/br_UI改修/target/classes")
        )).isTrue();
    }

    @Test
    void rejectsPackagedJarPathAsLocalDevelopmentExecution() {
        assertThat(RequestAccessTokenService.isLocalDevelopmentClassesPath(
                Path.of("C:/academia/src/Q-Scout-for-Spring1/br_UI改修/target/q-scout-for-spring-0.1.0-SNAPSHOT.jar")
        )).isFalse();
    }

    @Test
    void allowsEphemeralSecretForLocalDevelopmentExecution() {
        RequestAccessTokenService service = new RequestAccessTokenService(
                "",
                false,
                true,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-04-23T03:00:00Z"), ZoneOffset.UTC)
        );

        String url = service.createSignedUrl("/preview/req-1/human", "req-1", "human");

        assertThat(service.isValid(
                "req-1",
                "human",
                Long.parseLong(query(url, "expires")),
                query(url, "token")
        )).isTrue();
    }

    private String query(String url, String key) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst(key);
    }
}
