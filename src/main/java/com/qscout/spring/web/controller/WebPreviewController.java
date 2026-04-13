package com.qscout.spring.web.controller;

import com.qscout.spring.web.dto.PreviewArtifactView;
import com.qscout.spring.web.exception.ArtifactExpiredException;
import com.qscout.spring.web.service.DownloadArtifactService;
import com.qscout.spring.web.service.MarkdownPreviewRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class WebPreviewController {
    private final DownloadArtifactService downloadArtifactService;
    private final MarkdownPreviewRenderer markdownPreviewRenderer;

    public WebPreviewController(DownloadArtifactService downloadArtifactService, MarkdownPreviewRenderer markdownPreviewRenderer) {
        this.downloadArtifactService = downloadArtifactService;
        this.markdownPreviewRenderer = markdownPreviewRenderer;
    }

    @GetMapping("/preview/{requestId}/{fileKey}")
    public String preview(@PathVariable String requestId, @PathVariable String fileKey, Model model) {
        try {
            DownloadArtifactService.PreviewArtifact artifact = downloadArtifactService.resolveForPreview(requestId, fileKey);
            String renderedHtml = "human".equals(artifact.fileKey()) ? markdownPreviewRenderer.render(artifact.content()) : null;
            model.addAttribute("preview", new PreviewArtifactView(
                    requestId,
                    artifact.fileKey(),
                    artifact.fileName(),
                    artifact.content(),
                    "/download/" + requestId + "/" + fileKey,
                    renderedHtml
            ));
            return "preview";
        } catch (ArtifactExpiredException exception) {
            throw new ResponseStatusException(GONE, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
