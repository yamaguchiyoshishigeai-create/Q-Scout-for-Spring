package com.qscout.spring.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 成果物ダウンロードやプレビュー参照で使う一時アクセストークンを生成・検証するサービスである。
 *
 * <p>requestId と fileKey に基づく署名付き URL を組み立て、Web 入口での参照可否判定を補助する。</p>
 *
 * <p>認可入口の補助責務に留まり、成果物生成や成果物内容の解釈自体は担わない。</p>
 */
@Service
public class RequestAccessTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RequestAccessTokenService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final Duration tokenTtl;
    private final Clock clock;

    @Autowired
    public RequestAccessTokenService(
            @Value("${QSCOUT_ARTIFACT_TOKEN_SECRET:}") String configuredSecret,
            @Value("${qscout.artifact-token.allow-insecure-dev-secret:false}") boolean allowInsecureDevSecret,
            TempWorkspaceService tempWorkspaceService
    ) {
        this(resolveSecret(configuredSecret, allowInsecureDevSecret, isLocalDevelopmentExecution()),
                tempWorkspaceService.retention(),
                Clock.systemUTC());
    }

    RequestAccessTokenService(String configuredSecret, boolean allowInsecureDevSecret, Duration tokenTtl, Clock clock) {
        this(configuredSecret, allowInsecureDevSecret, false, tokenTtl, clock);
    }

    RequestAccessTokenService(
            String configuredSecret,
            boolean allowInsecureDevSecret,
            boolean localDevelopmentExecution,
            Duration tokenTtl,
            Clock clock
    ) {
        this(resolveSecret(configuredSecret, allowInsecureDevSecret, localDevelopmentExecution), tokenTtl, clock);
    }

    private RequestAccessTokenService(byte[] secret, Duration tokenTtl, Clock clock) {
        this.secret = secret;
        this.tokenTtl = tokenTtl;
        this.clock = clock;
    }

    /**
     * 追加クエリパラメータなしで、成果物参照用の署名付き URL を生成する。
     *
     * @param path 署名対象の URL パス
     * @param requestId 参照対象ワークスペースを識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @return 有効期限と署名トークンを含む署名付き URL
     */
    public String createSignedUrl(String path, String requestId, String fileKey) {
        return createSignedUrl(path, requestId, fileKey, Map.of());
    }

    /**
     * 追加クエリパラメータを含む、成果物参照用の署名付き URL を生成する。
     *
     * @param path 署名対象の URL パス
     * @param requestId 参照対象ワークスペースを識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @param extraQueryParameters 署名付き URL に付与する追加クエリパラメータ
     * @return 有効期限と署名トークンを含む署名付き URL
     */
    public String createSignedUrl(String path, String requestId, String fileKey, Map<String, String> extraQueryParameters) {
        long expires = clock.instant().plus(tokenTtl).getEpochSecond();
        String token = sign(requestId, fileKey, expires);
        Map<String, String> queryParameters = new LinkedHashMap<>(extraQueryParameters);
        queryParameters.put("expires", Long.toString(expires));
        queryParameters.put("token", token);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        queryParameters.forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    /**
     * 受け取った署名付き URL 情報が、現在も有効な成果物参照要求かどうかを判定する。
     *
     * @param requestId 参照対象ワークスペースを識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @param expires 署名付き URL の有効期限エポック秒
     * @param token requestId と fileKey に対する署名トークン
     * @return 有効期限内かつ署名が一致する場合は {@code true}
     */
    public boolean isValid(String requestId, String fileKey, long expires, String token) {
        if (token == null || token.isBlank() || expires <= 0) {
            return false;
        }
        Instant now = clock.instant();
        Instant expiry = Instant.ofEpochSecond(expires);
        if (expiry.isBefore(now)) {
            return false;
        }
        if (expiry.isAfter(now.plus(tokenTtl))) {
            return false;
        }
        String expected = sign(requestId, fileKey, expires);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String requestId, String fileKey, long expires) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal((requestId + ":" + fileKey + ":" + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to generate artifact access token.", exception);
        }
    }

    private static byte[] resolveSecret(
            String configuredSecret,
            boolean allowInsecureDevSecret,
            boolean localDevelopmentExecution
    ) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (allowInsecureDevSecret) {
            String ephemeralSecret = UUID.randomUUID().toString();
            logger.warn("Artifact token secret is missing. reasonCode=INSECURE_DEV_SECRET_ENABLED");
            return ephemeralSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (localDevelopmentExecution) {
            String ephemeralSecret = UUID.randomUUID().toString();
            logger.warn("Artifact token secret is missing. reasonCode=LOCAL_DEV_SECRET_FALLBACK_ENABLED");
            return ephemeralSecret.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalStateException(
                "QSCOUT_ARTIFACT_TOKEN_SECRET must be set or qscout.artifact-token.allow-insecure-dev-secret=true must be enabled for local development."
        );
    }

    static boolean isLocalDevelopmentExecution() {
        try {
            URL location = RequestAccessTokenService.class.getProtectionDomain().getCodeSource().getLocation();
            return isLocalDevelopmentClassesPath(Path.of(location.toURI()));
        } catch (URISyntaxException | NullPointerException | IllegalArgumentException exception) {
            return false;
        }
    }

    static boolean isLocalDevelopmentClassesPath(Path location) {
        if (location == null) {
            return false;
        }
        String normalized = location.normalize().toString().replace('\\', '/');
        return normalized.endsWith("/target/classes");
    }
}
