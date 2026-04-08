package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TempWorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(TempWorkspaceService.class);
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[0-9a-fA-F-]{36}$");
    private static final Duration RETENTION = Duration.ofMinutes(15);

    private final Path baseDir = Path.of(System.getProperty("java.io.tmpdir"), "qscout");

    public WorkspaceContext createWorkspace() {
        cleanupExpiredWorkspaces();
        String requestId = UUID.randomUUID().toString();
        Path rootDir = baseDir.resolve(requestId).normalize();
        Path uploadZipPath = rootDir.resolve("upload.zip");
        Path extractedDir = rootDir.resolve("extracted");
        Path outputDir = rootDir.resolve("output");
        try {
            Files.createDirectories(extractedDir);
            Files.createDirectories(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create temporary workspace.", exception);
        }
        return new WorkspaceContext(requestId, rootDir, uploadZipPath, extractedDir, outputDir, Instant.now());
    }

    public void cleanupNow(WorkspaceContext workspace) {
        logger.info("Cleaning up workspace. requestId={}, rootDir={}", workspace.requestId(), workspace.rootDir());
        deleteRecursively(workspace.rootDir());
    }

    public Path resolveWorkspaceRoot(String requestId) {
        if (!REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            throw new IllegalArgumentException("Invalid request id.");
        }
        return baseDir.resolve(requestId).normalize();
    }

    public boolean isExpired(String requestId) {
        Path rootDir = resolveWorkspaceRoot(requestId);
        if (!Files.exists(rootDir)) {
            return true;
        }
        try {
            Instant lastModified = Files.getLastModifiedTime(rootDir).toInstant();
            return lastModified.plus(RETENTION).isBefore(Instant.now());
        } catch (IOException exception) {
            return true;
        }
    }

    public void assertActive(String requestId) {
        if (isExpired(requestId)) {
            Path rootDir = resolveWorkspaceRoot(requestId);
            deleteRecursively(rootDir);
            throw new ArtifactExpiredException("ダウンロード期限が切れました。再度解析を実行してください。");
        }
    }

    private void cleanupExpiredWorkspaces() {
        if (!Files.exists(baseDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
            for (Path candidate : stream) {
                if (Files.isDirectory(candidate)) {
                    String requestId = candidate.getFileName().toString();
                    if (REQUEST_ID_PATTERN.matcher(requestId).matches() && isExpired(requestId)) {
                        logger.info("Removing expired workspace. requestId={}, rootDir={}", requestId, candidate);
                        deleteRecursively(candidate);
                    }
                }
            }
        } catch (IOException exception) {
            logger.warn("Failed to scan temporary workspace directory for cleanup. baseDir={}", baseDir, exception);
            // Best effort cleanup only.
        }
    }

    private void deleteRecursively(Path rootDir) {
        if (rootDir == null || !Files.exists(rootDir)) {
            return;
        }
        try (var paths = Files.walk(rootDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    logger.warn("Failed to delete temporary workspace path. path={}", path, exception);
                    // Best effort cleanup only.
                }
            });
        } catch (IOException exception) {
            logger.warn("Failed to walk temporary workspace for cleanup. rootDir={}", rootDir, exception);
            // Best effort cleanup only.
        }
    }

    public record WorkspaceContext(
            String requestId,
            Path rootDir,
            Path uploadZipPath,
            Path extractedDir,
            Path outputDir,
            Instant createdAt
    ) {
    }
}
