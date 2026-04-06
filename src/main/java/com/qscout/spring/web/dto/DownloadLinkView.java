package com.qscout.spring.web.dto;

public record DownloadLinkView(
        String label,
        String url,
        String fileName
) {
}
