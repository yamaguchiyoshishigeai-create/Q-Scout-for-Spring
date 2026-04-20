package com.qscout.spring.web.controller;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.RequestAccessTokenService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 生成済み成果物のダウンロード要求を受け付ける Web 入口 Controller である。
 *
 * <p>署名付き URL の検証を行い、成果物取得は {@link DownloadArtifactService} へ委譲して
 * ダウンロード応答を組み立てる。</p>
 *
 * <p>生成済み成果物を返す入口に留まり、成果物生成本体や解析本体は担わない。</p>
 */
@RestController
public class WebDownloadController {
    private final DownloadArtifactService downloadArtifactService;
    private final RequestAccessTokenService requestAccessTokenService;

    public WebDownloadController(DownloadArtifactService downloadArtifactService, RequestAccessTokenService requestAccessTokenService) {
        this.downloadArtifactService = downloadArtifactService;
        this.requestAccessTokenService = requestAccessTokenService;
    }

    /**
     * 署名付き URL で指定された成果物をダウンロード応答として返す。
     *
     * @param requestId 成果物保持先を識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @param expires 署名付き URL の有効期限エポック秒
     * @param token requestId と fileKey に対する署名トークン
     * @return 成果物リソースを含むダウンロード応答
     * @throws ResponseStatusException トークン不正、成果物期限切れ、または対象成果物が存在しない場合
     */
    @GetMapping("/download/{requestId}/{fileKey}")
    public ResponseEntity<Resource> download(
            @PathVariable String requestId,
            @PathVariable String fileKey,
            @RequestParam long expires,
            @RequestParam String token
    ) {
        try {
            if (!requestAccessTokenService.isValid(requestId, fileKey, expires, token)) {
                throw new ResponseStatusException(FORBIDDEN, "Invalid artifact access token.");
            }
            DownloadArtifactService.DownloadArtifact artifact = downloadArtifactService.resolveForDownload(requestId, fileKey);
            return ResponseEntity.ok()
                    .contentType(artifact.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(artifact.fileName()).build().toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(artifact.resource());
        } catch (ArtifactExpiredException exception) {
            throw new ResponseStatusException(GONE, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
