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
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Web から受け取った解析要求を共通解析へ接続し、画面表示用レスポンスを組み立てるサービスである。
 *
 * <p>アップロード検証、一時ワークスペース準備、zip 展開、タイムアウト付き解析実行、
 * ダウンロード導線の組み立てをまとめて扱う。</p>
 *
 * <p>画面描画そのものは controller / view に委ね、解析アルゴリズム本体は
 * {@link SharedAnalysisService} へ委譲する。</p>
 */
@Service
public class WebAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(WebAnalysisService.class);
    private static final int MAX_EXECUTION_SECONDS = 60;
    private static final DateTimeFormatter EXECUTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UploadValidationService uploadValidationService;
    private final TempWorkspaceService tempWorkspaceService;
    private final ZipExtractionService zipExtractionService;
    private final SharedAnalysisService sharedAnalysisService;
    private final RequestAccessTokenService requestAccessTokenService;
    private final MessageSource messageSource;

    public WebAnalysisService(
            UploadValidationService uploadValidationService,
            TempWorkspaceService tempWorkspaceService,
            ZipExtractionService zipExtractionService,
            SharedAnalysisService sharedAnalysisService,
            RequestAccessTokenService requestAccessTokenService,
            MessageSource messageSource
    ) {
        this.uploadValidationService = uploadValidationService;
        this.tempWorkspaceService = tempWorkspaceService;
        this.zipExtractionService = zipExtractionService;
        this.sharedAnalysisService = sharedAnalysisService;
        this.requestAccessTokenService = requestAccessTokenService;
        this.messageSource = messageSource;
    }

    /**
     * アップロードされたプロジェクト ZIP を解析し、Web 画面で利用する結果情報を返す。
     *
     * @param projectZip 解析対象プロジェクトを含むアップロード ZIP
     * @return スコア概要、ダウンロード導線、プレビュー導線を含む Web 応答
     * @throws AnalysisTimeoutException 解析が制限時間内に完了しなかった場合
     */
    public WebAnalysisResponse analyze(MultipartFile projectZip) {
        uploadValidationService.validate(projectZip);
        TempWorkspaceService.WorkspaceContext workspace = tempWorkspaceService.createWorkspace();
        try {
            zipExtractionService.saveUpload(projectZip, workspace.uploadZipPath());
            ZipExtractionResult extractionResult = zipExtractionService.extract(workspace.uploadZipPath(), workspace.extractedDir());
            Path projectRoot = zipExtractionService.resolveProjectRoot(workspace.extractedDir());
            SharedAnalysisService.SharedAnalysisResult result = executeWithTimeout(
                    new AnalysisRequest(projectRoot, workspace.outputDir())
            );
            String language = MessageSources.resolveLocale().getLanguage();
            return new WebAnalysisResponse(
                    workspace.requestId(),
                    originalFileName(projectZip),
                    formatExecutedAt(Instant.now()),
                    result.scoreSummary().finalScore(),
                    result.scoreSummary().totalViolations(),
                    result.scoreSummary().highCount(),
                    result.scoreSummary().mediumCount(),
                    result.scoreSummary().lowCount(),
                    new DownloadLinkView(
                            message("result.download.human"),
                            requestAccessTokenService.createSignedUrl("/download/" + workspace.requestId() + "/human", workspace.requestId(), "human"),
                            "qscout-report.md"
                    ),
                    new DownloadLinkView(
                            message("result.download.ai"),
                            requestAccessTokenService.createSignedUrl("/download/" + workspace.requestId() + "/ai", workspace.requestId(), "ai"),
                            "qscout-ai-input.md"
                    ),
                    requestAccessTokenService.createSignedUrl(
                            "/preview/" + workspace.requestId() + "/human",
                            workspace.requestId(),
                            "human",
                            Map.of("lang", language)
                    ),
                    requestAccessTokenService.createSignedUrl(
                            "/preview/" + workspace.requestId() + "/ai",
                            workspace.requestId(),
                            "ai",
                            Map.of("lang", language)
                    ),
                    message("result.completed"),
                    buildAutoExcludedMessage(extractionResult),
                    false,
                    true
            );
        } catch (RuntimeException exception) {
            logger.warn("Web analysis failed. requestId={}, reasonCode={}", workspace.requestId(), reasonCode(exception));
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

    private String buildAutoExcludedMessage(ZipExtractionResult extractionResult) {
        if (extractionResult == null || !extractionResult.hasAutoExcludedEntries()) {
            return null;
        }
        String skippedTargets = String.join(", ", extractionResult.skippedDirectoryNames());
        return message("result.autoExcluded.notice", extractionResult.skippedEntryCount(), skippedTargets);
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }

    private String reasonCode(RuntimeException exception) {
        if (exception instanceof AnalysisTimeoutException) {
            return "ANALYSIS_TIMEOUT";
        }
        return "ANALYSIS_FAILED";
    }
}
