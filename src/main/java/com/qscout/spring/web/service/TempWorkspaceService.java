package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

@Service
public class TempWorkspaceService {
    private static final Logger logger = LoggerFactory.getLogger(TempWorkspaceService.class);
    private static final String SCORE_BAND_FILE_NAME = "score-band.txt";

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
        logger.info("Cleaning up workspace. requestId={}, reasonCode=WORKSPACE_CLEANUP", workspace.requestId());
        deleteRecursively(workspace.rootDir());
    }

    public void storeScoreBandClass(WorkspaceContext workspace, String scoreBandClass) {
        if (workspace == null || scoreBandClass == null || scoreBandClass.isBlank()) {
            return;
        }
        Path scoreBandPath = workspace.rootDir().resolve(SCORE_BAND_FILE_NAME).normalize();
        if (!scoreBandPath.startsWith(workspace.rootDir())) {
            throw new IllegalArgumentException("Invalid score band path.");
        }
        try {
            Files.writeString(scoreBandPath, scoreBandClass, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist workspace score band.", exception);
        }
    }

    public Path resolveWorkspaceRoot(String requestId) {
        if (!isCanonicalRequestId(requestId)) {
            throw new IllegalArgumentException("Invalid request id.");
        }
        return baseDir.resolve(requestId).normalize();
    }

    public String readScoreBandClass(String requestId) {
        Path rootDir = resolveWorkspaceRoot(requestId);
        Path scoreBandPath = rootDir.resolve(SCORE_BAND_FILE_NAME).normalize();
        if (!scoreBandPath.startsWith(rootDir) || !Files.exists(scoreBandPath) || !Files.isRegularFile(scoreBandPath)) {
            return null;
        }
        try {
            String scoreBandClass = Files.readString(scoreBandPath, StandardCharsets.UTF_8).trim();
            return scoreBandClass.isBlank() ? null : scoreBandClass;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read workspace score band.", exception);
        }
    }

    public boolean isExpired(String requestId) {
        Path rootDir = resolveWorkspaceRoot(requestId);
        if (!Files.exists(rootDir)) {
            return true;
        }
        try {
            Instant lastModified = Files.getLastModifiedTime(rootDir).toInstant();
            return lastModified.plus(retention()).isBefore(Instant.now());
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

    public Duration retention() {
        return ZipSecurityLimits.WORKSPACE_RETENTION;
    }

    private void cleanupExpiredWorkspaces() {
        if (!Files.exists(baseDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
            for (Path candidate : stream) {
                if (Files.isDirectory(candidate)) {
                    String requestId = candidate.getFileName().toString();
                    if (isCanonicalRequestId(requestId) && isExpired(requestId)) {
                        logger.info("Removing expired workspace. requestId={}, reasonCode=WORKSPACE_EXPIRED", requestId);
                        deleteRecursively(candidate);
                    }
                }
            }
        } catch (IOException exception) {
            logger.warn("Failed to scan temporary workspace directory for cleanup. reasonCode=WORKSPACE_SCAN_FAILED");
            // Best effort cleanup only.
        }
    }

    private void deleteRecursively(Path rootDir) {
        if (rootDir == null || !Files.exists(rootDir)) {
            return;
        }
        String requestId = requestIdOf(rootDir);
        try (var paths = Files.walk(rootDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    logger.warn("Failed to delete temporary workspace path. requestId={}, reasonCode=WORKSPACE_DELETE_FAILED, entry={}",
                            requestId,
                            path.getFileName());
                    // Best effort cleanup only.
                }
            });
        } catch (IOException exception) {
            logger.warn("Failed to walk temporary workspace for cleanup. requestId={}, reasonCode=WORKSPACE_WALK_FAILED", requestId);
            // Best effort cleanup only.
        }
    }

    private boolean isCanonicalRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return false;
        }
        try {
            return UUID.fromString(requestId).toString().equals(requestId.toLowerCase(Locale.ROOT))
                    && requestId.equals(requestId.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String requestIdOf(Path rootDir) {
        if (rootDir == null || rootDir.getFileName() == null) {
            return "unknown";
        }
        return rootDir.getFileName().toString();
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
