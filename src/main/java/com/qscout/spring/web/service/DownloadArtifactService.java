package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class DownloadArtifactService {
    private static final Map<String, String> FILE_MAPPING = Map.of(
            "human", "qscout-report.md",
            "ai", "qscout-ai-input.md"
    );

    private final TempWorkspaceService tempWorkspaceService;

    public DownloadArtifactService(TempWorkspaceService tempWorkspaceService) {
        this.tempWorkspaceService = tempWorkspaceService;
    }

    public DownloadArtifact resolveForDownload(String requestId, String fileKey) {
        String fileName = FILE_MAPPING.get(fileKey);
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid download file key.");
        }

        tempWorkspaceService.assertActive(requestId);
        Path rootDir = tempWorkspaceService.resolveWorkspaceRoot(requestId);
        Path artifactPath = rootDir.resolve("output").resolve(fileName).normalize();
        if (!artifactPath.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid download path.");
        }
        if (!Files.exists(artifactPath) || !Files.isRegularFile(artifactPath)) {
            throw new ArtifactExpiredException("ダウンロード期限が切れました。再度解析を実行してください。");
        }
        Resource resource = new FileSystemResource(artifactPath);
        return new DownloadArtifact(resource, fileName, MediaType.TEXT_MARKDOWN);
    }

    public record DownloadArtifact(
            Resource resource,
            String fileName,
            MediaType contentType
    ) {
    }
}
