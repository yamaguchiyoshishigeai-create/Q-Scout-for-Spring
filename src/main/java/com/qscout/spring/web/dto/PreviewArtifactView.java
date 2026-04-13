package com.qscout.spring.web.dto;

public record PreviewArtifactView(
        String requestId,
        String fileKey,
        String fileName,
        String content,
        String downloadUrl,
        String renderedHtml
) {
}
