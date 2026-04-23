package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadArtifactServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesKnownArtifact() throws IOException {
        TempWorkspaceService tempWorkspaceService = mock(TempWorkspaceService.class);
        DownloadArtifactService service = new DownloadArtifactService(tempWorkspaceService);
        Path workspaceRoot = tempDir.resolve("request");
        Path outputDir = workspaceRoot.resolve("output");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("qscout-report.md"), "report");

        when(tempWorkspaceService.resolveWorkspaceRoot("req-1")).thenReturn(workspaceRoot);

        DownloadArtifactService.DownloadArtifact artifact = service.resolveForDownload("req-1", "human");

        verify(tempWorkspaceService).assertActive("req-1");
        assertThat(artifact.fileName()).isEqualTo("qscout-report.md");
        assertThat(((Resource) artifact.resource()).exists()).isTrue();
    }

    @Test
    void resolvesPreviewContent() throws IOException {
        TempWorkspaceService tempWorkspaceService = mock(TempWorkspaceService.class);
        DownloadArtifactService service = new DownloadArtifactService(tempWorkspaceService);
        Path workspaceRoot = tempDir.resolve("request-preview");
        Path outputDir = workspaceRoot.resolve("output");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("qscout-ai-input.md"), "# preview\ncontent");

        when(tempWorkspaceService.resolveWorkspaceRoot("req-2")).thenReturn(workspaceRoot);
        when(tempWorkspaceService.readScoreBandClass("req-2")).thenReturn("score-band-low");

        DownloadArtifactService.PreviewArtifact artifact = service.resolveForPreview("req-2", "ai");

        verify(tempWorkspaceService).assertActive("req-2");
        assertThat(artifact.fileName()).isEqualTo("qscout-ai-input.md");
        assertThat(artifact.content()).contains("# preview");
        assertThat(artifact.scoreBandClass()).isEqualTo("score-band-low");
    }

    @Test
    void rejectsUnknownKey() {
        DownloadArtifactService service = new DownloadArtifactService(mock(TempWorkspaceService.class));

        assertThatThrownBy(() -> service.resolveForDownload("req-1", "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void treatsMissingFileAsExpired() {
        TempWorkspaceService tempWorkspaceService = mock(TempWorkspaceService.class);
        DownloadArtifactService service = new DownloadArtifactService(tempWorkspaceService);
        when(tempWorkspaceService.resolveWorkspaceRoot("req-1")).thenReturn(tempDir.resolve("missing"));

        assertThatThrownBy(() -> service.resolveForDownload("req-1", "human"))
                .isInstanceOf(ArtifactExpiredException.class);
    }
}
