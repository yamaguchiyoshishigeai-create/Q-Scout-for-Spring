package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class DownloadArtifactService {
    private static final Map<String, ArtifactDefinition> FILE_MAPPING = Map.of(
            "human", new ArtifactDefinition("qscout-report.md", MediaType.TEXT_MARKDOWN),
            "ai", new ArtifactDefinition("qscout-ai-input.md", MediaType.TEXT_MARKDOWN)
    );

    private final TempWorkspaceService tempWorkspaceService;

    public DownloadArtifactService(TempWorkspaceService tempWorkspaceService) {
        this.tempWorkspaceService = tempWorkspaceService;
    }

    public DownloadArtifact resolveForDownload(String requestId, String fileKey) {
        ResolvedArtifact artifact = resolveArtifact(requestId, fileKey);
        Resource resource = new FileSystemResource(artifact.path());
        return new DownloadArtifact(resource, artifact.fileName(), artifact.contentType());
    }

    public PreviewArtifact resolveForPreview(String requestId, String fileKey) {
        ResolvedArtifact artifact = resolveArtifact(requestId, fileKey);
        try {
            String content = Files.readString(artifact.path(), StandardCharsets.UTF_8);
            return new PreviewArtifact(artifact.fileKey(), artifact.fileName(), artifact.contentType(), content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read preview artifact.", exception);
        }
    }

    private ResolvedArtifact resolveArtifact(String requestId, String fileKey) {
        ArtifactDefinition definition = FILE_MAPPING.get(fileKey);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid download file key.");
        }

        tempWorkspaceService.assertActive(requestId);
        Path rootDir = tempWorkspaceService.resolveWorkspaceRoot(requestId);
        Path artifactPath = rootDir.resolve("output").resolve(definition.fileName()).normalize();
        if (!artifactPath.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid download path.");
        }
        if (!Files.exists(artifactPath) || !Files.isRegularFile(artifactPath)) {
            throw new ArtifactExpiredException("ダウンロード期限が切れました。再度解析を実行してください。");
        }
        return new ResolvedArtifact(fileKey, artifactPath, definition.fileName(), definition.contentType());
    }

    private record ArtifactDefinition(
            String fileName,
            MediaType contentType
    ) {
    }

    private record ResolvedArtifact(
            String fileKey,
            Path path,
            String fileName,
            MediaType contentType
    ) {
    }

    public record DownloadArtifact(
            Resource resource,
            String fileName,
            MediaType contentType
    ) {
    }

    public record PreviewArtifact(
            String fileKey,
            String fileName,
            MediaType contentType,
            String content
    ) {
    }
}
