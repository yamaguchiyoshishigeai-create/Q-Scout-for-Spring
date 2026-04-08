package com.qscout.spring.web.service;

import com.qscout.spring.application.SharedAnalysisService;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.web.dto.DownloadLinkView;
import com.qscout.spring.web.dto.WebAnalysisResponse;
import com.qscout.spring.web.exception.AnalysisTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class WebAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(WebAnalysisService.class);
    private static final int MAX_EXECUTION_SECONDS = 60;

    private final UploadValidationService uploadValidationService;
    private final TempWorkspaceService tempWorkspaceService;
    private final ZipExtractionService zipExtractionService;
    private final SharedAnalysisService sharedAnalysisService;
    private final MessageSource messageSource;

    public WebAnalysisService(
            UploadValidationService uploadValidationService,
            TempWorkspaceService tempWorkspaceService,
            ZipExtractionService zipExtractionService,
            SharedAnalysisService sharedAnalysisService,
            MessageSource messageSource
    ) {
        this.uploadValidationService = uploadValidationService;
        this.tempWorkspaceService = tempWorkspaceService;
        this.zipExtractionService = zipExtractionService;
        this.sharedAnalysisService = sharedAnalysisService;
        this.messageSource = messageSource;
    }

    public WebAnalysisResponse analyze(MultipartFile projectZip) {
        uploadValidationService.validate(projectZip);
        TempWorkspaceService.WorkspaceContext workspace = tempWorkspaceService.createWorkspace();
        Locale locale = LocaleContextHolder.getLocale();
        try {
            zipExtractionService.saveUpload(projectZip, workspace.uploadZipPath());
            zipExtractionService.extract(workspace.uploadZipPath(), workspace.extractedDir());
            Path projectRoot = zipExtractionService.resolveProjectRoot(workspace.extractedDir());
            SharedAnalysisService.SharedAnalysisResult result = executeWithTimeout(
                    new AnalysisRequest(projectRoot, workspace.outputDir()),
                    locale
            );
            return new WebAnalysisResponse(
                    workspace.requestId(),
                    result.scoreSummary().finalScore(),
                    result.scoreSummary().totalViolations(),
                    result.scoreSummary().highCount(),
                    result.scoreSummary().mediumCount(),
                    result.scoreSummary().lowCount(),
                    new DownloadLinkView(message("download.human.label", locale), "/download/" + workspace.requestId() + "/human", "qscout-report.md"),
                    new DownloadLinkView(message("download.ai.label", locale), "/download/" + workspace.requestId() + "/ai", "qscout-ai-input.md"),
                    message("success.analysis.completed", locale),
                    false,
                    true
            );
        } catch (RuntimeException exception) {
            logger.warn("Web analysis failed. requestId={}", workspace.requestId(), exception);
            tempWorkspaceService.cleanupNow(workspace);
            throw exception;
        }
    }

    private SharedAnalysisService.SharedAnalysisResult executeWithTimeout(AnalysisRequest request, Locale locale) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<SharedAnalysisService.SharedAnalysisResult> future = executor.submit(() -> sharedAnalysisService.execute(request, locale));
            return future.get(MAX_EXECUTION_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new AnalysisTimeoutException(
                    "error.timeout",
                    "解析が時間制限を超えました。より小さいプロジェクトで再試行してください。",
                    exception
            );
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("解析中に予期しないエラーが発生しました。", cause);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("解析が中断されました。", exception);
        } finally {
            executor.shutdownNow();
        }
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }
}
