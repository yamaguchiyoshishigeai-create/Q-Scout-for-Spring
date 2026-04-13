package com.qscout.spring.web.service;

import com.qscout.spring.application.SharedAnalysisService;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.i18n.MessageSources;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter EXECUTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
        try {
            zipExtractionService.saveUpload(projectZip, workspace.uploadZipPath());
            zipExtractionService.extract(workspace.uploadZipPath(), workspace.extractedDir());
            Path projectRoot = zipExtractionService.resolveProjectRoot(workspace.extractedDir());
            SharedAnalysisService.SharedAnalysisResult result = executeWithTimeout(
                    new AnalysisRequest(projectRoot, workspace.outputDir())
            );
            return new WebAnalysisResponse(
                    workspace.requestId(),
                    originalFileName(projectZip),
                    formatExecutedAt(Instant.now()),
                    result.scoreSummary().finalScore(),
                    result.scoreSummary().totalViolations(),
                    result.scoreSummary().highCount(),
                    result.scoreSummary().mediumCount(),
                    result.scoreSummary().lowCount(),
                    new DownloadLinkView(message("result.download.human"), "/download/" + workspace.requestId() + "/human", "qscout-report.md"),
                    new DownloadLinkView(message("result.download.ai"), "/download/" + workspace.requestId() + "/ai", "qscout-ai-input.md"),
                    message("result.completed"),
                    false,
                    true
            );
        } catch (RuntimeException exception) {
            logger.warn("Web analysis failed. requestId={}", workspace.requestId(), exception);
            tempWorkspaceService.cleanupNow(workspace);
            throw exception;
        }
    }

    private SharedAnalysisService.SharedAnalysisResult executeWithTimeout(AnalysisRequest request) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Locale locale = MessageSources.resolveLocale();
        try {
            Future<SharedAnalysisService.SharedAnalysisResult> future = executor.submit(() -> {
                Locale previous = LocaleContextHolder.getLocale();
                LocaleContextHolder.setLocale(locale);
                try {
                    return sharedAnalysisService.execute(request);
                } finally {
                    LocaleContextHolder.setLocale(previous);
                }
            });
            return future.get(MAX_EXECUTION_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new AnalysisTimeoutException(message("error.analysis.timeout"), exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(message("error.analysis.execution"), cause);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(message("error.analysis.interrupted"), exception);
        } finally {
            executor.shutdownNow();
        }
    }

    private String originalFileName(MultipartFile projectZip) {
        String originalName = projectZip != null ? projectZip.getOriginalFilename() : null;
        if (originalName == null || originalName.isBlank()) {
            return message("result.targetFile.unknown");
        }
        String normalized = originalName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return fileName.isEmpty() ? message("result.targetFile.unknown") : fileName;
    }

    private String formatExecutedAt(Instant executedAt) {
        return EXECUTED_AT_FORMAT.withLocale(MessageSources.resolveLocale())
                .format(executedAt.atZone(ZoneId.systemDefault()));
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }
}
