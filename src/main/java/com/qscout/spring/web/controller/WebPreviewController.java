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
