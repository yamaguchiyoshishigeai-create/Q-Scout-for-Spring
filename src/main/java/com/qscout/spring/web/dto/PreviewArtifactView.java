package com.qscout.spring.web.dto;

public record PreviewArtifactView(
        String requestId,
        String fileKey,
        String fileName,
        String scoreBandClass,
        String content,
        String downloadUrl,
        String japanesePreviewUrl,
        String englishPreviewUrl,
        String renderedHtml
) {
}
