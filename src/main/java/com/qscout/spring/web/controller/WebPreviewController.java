package com.qscout.spring.web.controller;

import com.qscout.spring.web.dto.PreviewArtifactView;
import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.MarkdownPreviewRenderer;
import com.qscout.spring.web.service.RequestAccessTokenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 生成済み成果物のプレビュー表示入口を担う Controller である。
 *
 * <p>アクセス用トークン検証、成果物参照、Markdown の HTML 化、
 * プレビュー画面用 Model 構築を扱う。</p>
 *
 * <p>成果物生成本体は持たず、既に生成済みの内容を安全に参照させる導線に専念する。</p>
 */
@Controller
public class WebPreviewController {
    private final DownloadArtifactService downloadArtifactService;
    private final MarkdownPreviewRenderer markdownPreviewRenderer;
    private final RequestAccessTokenService requestAccessTokenService;

    public WebPreviewController(
            DownloadArtifactService downloadArtifactService,
            MarkdownPreviewRenderer markdownPreviewRenderer,
            RequestAccessTokenService requestAccessTokenService
    ) {
        this.downloadArtifactService = downloadArtifactService;
        this.markdownPreviewRenderer = markdownPreviewRenderer;
        this.requestAccessTokenService = requestAccessTokenService;
    }

    /**
     * 署名付き URL から成果物を取得し、プレビュー画面用 Model を構築する。
     *
     * @param requestId 成果物保持先を識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @param expires 署名付き URL の有効期限エポック秒
     * @param token requestId と fileKey に対する署名トークン
     * @param model プレビュー表示情報を格納する Model
     * @param response キャッシュ抑止ヘッダーを設定する HTTP レスポンス
     * @return プレビュー画面のテンプレート名
     * @throws ResponseStatusException トークン不正、成果物期限切れ、または対象成果物が存在しない場合
     */
    @GetMapping("/preview/{requestId}/{fileKey}")
    public String preview(
            @PathVariable String requestId,
            @PathVariable String fileKey,
            @RequestParam long expires,
            @RequestParam String token,
            Model model,
            HttpServletResponse response
    ) {
        try {
            if (!requestAccessTokenService.isValid(requestId, fileKey, expires, token)) {
                throw new ResponseStatusException(FORBIDDEN, "Invalid artifact access token.");
            }
            applyNoStore(response);
            DownloadArtifactService.PreviewArtifact artifact = downloadArtifactService.resolveForPreview(requestId, fileKey);
            String renderedHtml = "human".equals(artifact.fileKey()) ? markdownPreviewRenderer.render(artifact.content()) : null;
            model.addAttribute("preview", new PreviewArtifactView(
                    requestId,
                    artifact.fileKey(),
                    artifact.fileName(),
                    artifact.content(),
                    requestAccessTokenService.createSignedUrl("/download/" + requestId + "/" + fileKey, requestId, fileKey),
                    requestAccessTokenService.createSignedUrl(
                            "/preview/" + requestId + "/" + fileKey,
                            requestId,
                            fileKey,
                            Map.of("lang", "ja")
                    ),
                    requestAccessTokenService.createSignedUrl(
                            "/preview/" + requestId + "/" + fileKey,
                            requestId,
                            fileKey,
                            Map.of("lang", "en")
                    ),
                    renderedHtml
            ));
            return "preview";
        } catch (ArtifactExpiredException exception) {
            throw new ResponseStatusException(GONE, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(NOT_FOUND, exception.getMessage(), exception);
        }
    }

    private void applyNoStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}
