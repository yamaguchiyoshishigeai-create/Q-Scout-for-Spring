package com.qscout.spring.web.service;

import com.qscout.spring.application.SharedAnalysisService;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.ReportArtifact;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.i18n.MessageSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebAnalysisServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void analyzesZipAndBuildsDownloadLinks() {
        UploadValidationService uploadValidationService = mock(UploadValidationService.class);
        TempWorkspaceService tempWorkspaceService = mock(TempWorkspaceService.class);
        ZipExtractionService zipExtractionService = mock(ZipExtractionService.class);
        SharedAnalysisService sharedAnalysisService = mock(SharedAnalysisService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        WebAnalysisService service = new WebAnalysisService(uploadValidationService, tempWorkspaceService, zipExtractionService, sharedAnalysisService, requestAccessTokenService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "sample.zip", "application/zip", new byte[]{1, 2, 3});
        TempWorkspaceService.WorkspaceContext workspace = new TempWorkspaceService.WorkspaceContext(
                "req-123",
                tempDir.resolve("workspace"),
                tempDir.resolve("workspace/upload.zip"),
                tempDir.resolve("workspace/extracted"),
                tempDir.resolve("workspace/output"),
                Instant.now()
        );
        Path projectRoot = tempDir.resolve("project");
        ScoreSummary scoreSummary = new ScoreSummary(100, 85, 1, 2, 3, 6);
        SharedAnalysisService.SharedAnalysisResult result = new SharedAnalysisService.SharedAnalysisResult(
                new AnalysisResult(new ProjectContext(projectRoot, projectRoot.resolve("pom.xml"), List.<Path>of(), List.<Path>of()), List.<RuleResult>of(), List.of()),
                scoreSummary,
                new ReportArtifact(tempDir.resolve("workspace/output/qscout-report.md"), tempDir.resolve("workspace/output/qscout-ai-input.md"))
        );

        when(tempWorkspaceService.createWorkspace()).thenReturn(workspace);
        when(zipExtractionService.resolveProjectRoot(workspace.extractedDir())).thenReturn(projectRoot);
        when(sharedAnalysisService.execute(any(AnalysisRequest.class))).thenReturn(result);
        when(requestAccessTokenService.createSignedUrl("/download/req-123/human", "req-123", "human")).thenReturn("/download/req-123/human?expires=111&token=h");
        when(requestAccessTokenService.createSignedUrl("/download/req-123/ai", "req-123", "ai")).thenReturn("/download/req-123/ai?expires=111&token=a");
        when(requestAccessTokenService.createSignedUrl(eq("/preview/req-123/human"), eq("req-123"), eq("human"), anyMap())).thenReturn("/preview/req-123/human?lang=ja&expires=111&token=ph");
        when(requestAccessTokenService.createSignedUrl(eq("/preview/req-123/ai"), eq("req-123"), eq("ai"), anyMap())).thenReturn("/preview/req-123/ai?lang=ja&expires=111&token=pa");

        var response = service.analyze(file);

        verify(uploadValidationService).validate(file);
        verify(zipExtractionService).saveUpload(file, workspace.uploadZipPath());
        verify(zipExtractionService).extract(workspace.uploadZipPath(), workspace.extractedDir());
        assertThat(response.requestId()).isEqualTo("req-123");
        assertThat(response.originalFileName()).isEqualTo("sample.zip");
        assertThat(response.executedAt()).isNotBlank();
        assertThat(response.finalScore()).isEqualTo(85);
        assertThat(response.humanDownloadLink().url()).isEqualTo("/download/req-123/human?expires=111&token=h");
        assertThat(response.humanPreviewUrl()).isEqualTo("/preview/req-123/human?lang=ja&expires=111&token=ph");
    }

    @Test
    void fallsBackWhenOriginalFileNameIsMissing() {
        UploadValidationService uploadValidationService = mock(UploadValidationService.class);
        TempWorkspaceService tempWorkspaceService = mock(TempWorkspaceService.class);
        ZipExtractionService zipExtractionService = mock(ZipExtractionService.class);
        SharedAnalysisService sharedAnalysisService = mock(SharedAnalysisService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        WebAnalysisService service = new WebAnalysisService(uploadValidationService, tempWorkspaceService, zipExtractionService, sharedAnalysisService, requestAccessTokenService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "", "application/zip", new byte[]{1, 2, 3});
        TempWorkspaceService.WorkspaceContext workspace = new TempWorkspaceService.WorkspaceContext(
                "req-124",
                tempDir.resolve("workspace2"),
                tempDir.resolve("workspace2/upload.zip"),
                tempDir.resolve("workspace2/extracted"),
                tempDir.resolve("workspace2/output"),
                Instant.now()
        );
        Path projectRoot = tempDir.resolve("project2");
        ScoreSummary scoreSummary = new ScoreSummary(100, 85, 1, 2, 3, 6);
        SharedAnalysisService.SharedAnalysisResult result = new SharedAnalysisService.SharedAnalysisResult(
                new AnalysisResult(new ProjectContext(projectRoot, projectRoot.resolve("pom.xml"), List.<Path>of(), List.<Path>of()), List.<RuleResult>of(), List.of()),
                scoreSummary,
                new ReportArtifact(tempDir.resolve("workspace2/output/qscout-report.md"), tempDir.resolve("workspace2/output/qscout-ai-input.md"))
        );

        when(tempWorkspaceService.createWorkspace()).thenReturn(workspace);
        when(zipExtractionService.resolveProjectRoot(workspace.extractedDir())).thenReturn(projectRoot);
        when(sharedAnalysisService.execute(any(AnalysisRequest.class))).thenReturn(result);
        when(requestAccessTokenService.createSignedUrl("/download/req-124/human", "req-124", "human")).thenReturn("/download/req-124/human?expires=111&token=h");
        when(requestAccessTokenService.createSignedUrl("/download/req-124/ai", "req-124", "ai")).thenReturn("/download/req-124/ai?expires=111&token=a");
        when(requestAccessTokenService.createSignedUrl(eq("/preview/req-124/human"), eq("req-124"), eq("human"), anyMap())).thenReturn("/preview/req-124/human?lang=ja&expires=111&token=ph");
        when(requestAccessTokenService.createSignedUrl(eq("/preview/req-124/ai"), eq("req-124"), eq("ai"), anyMap())).thenReturn("/preview/req-124/ai?lang=ja&expires=111&token=pa");

        var response = service.analyze(file);

        assertThat(response.originalFileName()).isEqualTo("不明なzipファイル");
    }

    @Test
    void cleansWorkspaceWhenAnalysisFails() {
        UploadValidationService uploadValidationService = mock(UploadValidationService.class);
        TempWorkspaceService tempWorkspaceService = mock(TempWorkspaceService.class);
        ZipExtractionService zipExtractionService = mock(ZipExtractionService.class);
        SharedAnalysisService sharedAnalysisService = mock(SharedAnalysisService.class);
        RequestAccessTokenService requestAccessTokenService = mock(RequestAccessTokenService.class);
        WebAnalysisService service = new WebAnalysisService(uploadValidationService, tempWorkspaceService, zipExtractionService, sharedAnalysisService, requestAccessTokenService, MessageSources.create());
        MockMultipartFile file = new MockMultipartFile("projectZip", "sample.zip", "application/zip", new byte[]{1, 2, 3});
        TempWorkspaceService.WorkspaceContext workspace = new TempWorkspaceService.WorkspaceContext(
                "req-999",
                tempDir.resolve("failed"),
                tempDir.resolve("failed/upload.zip"),
                tempDir.resolve("failed/extracted"),
                tempDir.resolve("failed/output"),
                Instant.now()
        );

        when(tempWorkspaceService.createWorkspace()).thenReturn(workspace);
        doThrow(new IllegalStateException("boom")).when(zipExtractionService).extract(workspace.uploadZipPath(), workspace.extractedDir());

        assertThatThrownBy(() -> service.analyze(file)).isInstanceOf(IllegalStateException.class);
        verify(tempWorkspaceService).cleanupNow(workspace);
    }
}
