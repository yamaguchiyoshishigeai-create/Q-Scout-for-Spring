package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TempWorkspaceServiceTest {
    private final TempWorkspaceService service = new TempWorkspaceService();

    @Test
    void createsAndCleansWorkspace() {
        TempWorkspaceService.WorkspaceContext workspace = service.createWorkspace();
        try {
            assertThat(workspace.requestId()).matches("^[0-9a-fA-F-]{36}$");
            assertThat(Files.exists(workspace.extractedDir())).isTrue();
            assertThat(Files.exists(workspace.outputDir())).isTrue();
        } finally {
            service.cleanupNow(workspace);
            assertThat(Files.exists(workspace.rootDir())).isFalse();
        }
    }

    @Test
    void rejectsExpiredWorkspace() throws Exception {
        String requestId = UUID.randomUUID().toString();
        Path rootDir = service.resolveWorkspaceRoot(requestId);
        Files.createDirectories(rootDir);
        Files.setLastModifiedTime(rootDir, java.nio.file.attribute.FileTime.from(Instant.now().minusSeconds(3600)));

        try {
            assertThatThrownBy(() -> service.assertActive(requestId))
                    .isInstanceOf(ArtifactExpiredException.class)
                    .hasMessageContaining("ダウンロード期限");
            assertThat(Files.exists(rootDir)).isFalse();
        } finally {
            Files.deleteIfExists(rootDir);
        }
    }
}
