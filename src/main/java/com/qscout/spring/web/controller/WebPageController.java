package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.ErrorViewModel;
import com.qscout.spring.web.dto.ExecutionLimitView;
import com.qscout.spring.web.dto.DownloadLinkView;
import com.qscout.spring.web.dto.RateLimitDecision;
import com.qscout.spring.web.dto.SummaryDisplayView;
import com.qscout.spring.web.dto.UploadErrorModalView;
import com.qscout.spring.web.dto.WebAnalysisResponse;
import com.qscout.spring.web.exception.AnalysisTimeoutException;
import com.qscout.spring.web.exception.InvalidProjectStructureException;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.exception.UploadTooLargeException;
import com.qscout.spring.web.service.ClientIpResolver;
import com.qscout.spring.web.service.RequestRateLimiter;
import com.qscout.spring.web.service.WebAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * トップページ、ヘルプ画面、および Web 解析実行導線の入口を担う Controller である。
 *
 * <p>HTTP リクエストを受け取り、共通表示用 Model を構築しつつ
 * {@link WebAnalysisService} やレート制御サービスへ委譲する。</p>
 *
 * <p>解析本体や成果物生成は持たず、画面応答とエラーハンドリングの調停に留める。</p>
 */
@Controller
public class WebPageController {
    private static final Logger logger = LoggerFactory.getLogger(WebPageController.class);

    private final WebAnalysisService webAnalysisService;
    private final RequestRateLimiter requestRateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final MessageSource messageSource;

    public WebPageController(
            WebAnalysisService webAnalysisService,
            RequestRateLimiter requestRateLimiter,
            ClientIpResolver clientIpResolver,
            MessageSource messageSource
    ) {
        this.webAnalysisService = webAnalysisService;
        this.requestRateLimiter = requestRateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.messageSource = messageSource;
    }

    /**
     * トップページを表示する。
     *
     * @param model 画面共通情報を格納する Model
     * @return トップページのテンプレート名
     */
    @GetMapping("/")
    public String showIndex(Model model) {
        populateCommon(model);
        return "index";
    }

    /**
     * ヘルプ画面を表示する。
     *
     * @param model 画面共通情報を格納する Model
     * @return ヘルプ画面のテンプレート名
     */
    @GetMapping("/help")
    public String showHelp(Model model) {
        populateCommon(model);
        return "help";
    }

    /**
     * 解析対象 ZIP のアップロードを受け付け、結果表示またはエラー表示用の Model を組み立てる。
     *
     * @param file ユーザーが送信した解析対象 ZIP
     * @param model 解析結果またはエラー情報を格納する Model
     * @param request レート制御用のリクエスト情報
     * @param servletResponse レート制御時の HTTP 応答ヘッダーを設定するレスポンス
     * @return 結果表示を含むトップページのテンプレート名
     */
    @PostMapping("/analyze")
    public String analyze(
            @RequestParam(value = "projectZip", required = false) MultipartFile file,
            Model model,
            HttpServletRequest request,
            HttpServletResponse servletResponse,
            RedirectAttributes redirectAttributes
    ) {
        populateCommon(model);
        String clientIp = clientIpResolver.resolve(request);
        RateLimitDecision decision = requestRateLimiter.evaluate(clientIp);
        if (!decision.allowed()) {
            logger.warn("Analyze request rate limited. clientIp={}, reasonCode=RATE_LIMIT_EXCEEDED", clientIp);
            servletResponse.setStatus(429);
            servletResponse.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            model.addAttribute("error", new ErrorViewModel(message("error.rateLimit.exceeded"), "RATE_LIMIT_EXCEEDED", true));
            model.addAttribute("postAnalyzeAnchor", "run-analysis");
            return "index";
        }
        try {
            WebAnalysisResponse analysisResponse = webAnalysisService.analyze(file);
            redirectAttributes.addFlashAttribute("response", analysisResponse);
            redirectAttributes.addFlashAttribute("resultSummary", new SummaryDisplayView(analysisResponse, false, "#artifacts"));
            return "redirect:/#result-summary";
        } catch (UploadTooLargeException exception) {
            logger.warn("Upload exceeded size limit during controller validation. filename={}, reasonCode=UPLOAD_TOO_LARGE",
                    sanitizedFileName(file));
            redirectAttributes.addFlashAttribute("uploadErrorModal", new UploadErrorModalView(
                    message("error.upload.tooLarge.title"),
                    message("error.upload.tooLarge.body"),
                    message("error.upload.tooLarge.retry")
            ));
        } catch (InvalidUploadException | InvalidProjectStructureException exception) {
            logger.warn("Input validation failed during web analysis. filename={}, reasonCode=INPUT_ERROR",
                    sanitizedFileName(file));
            redirectAttributes.addFlashAttribute("error", new ErrorViewModel(exception.getMessage(), "INPUT_ERROR", true));
        } catch (AnalysisTimeoutException exception) {
            logger.warn("Web analysis timed out. filename={}, reasonCode=TIMEOUT", sanitizedFileName(file));
            redirectAttributes.addFlashAttribute("error", new ErrorViewModel(exception.getMessage(), "TIMEOUT", true));
        } catch (RuntimeException exception) {
            logger.error("Unexpected web analysis error.", exception);
            redirectAttributes.addFlashAttribute("error", new ErrorViewModel(message("error.unexpected"), "UNEXPECTED", true));
        }
        return "redirect:/#run-analysis";
    }

    private void populateCommon(Model model) {
        if (!model.containsAttribute("limits")) {
            model.addAttribute("limits", new ExecutionLimitView(20, 60, "zip"));
        }
        if (!model.containsAttribute("resultSummary")) {
            model.addAttribute("resultSummary", null);
        }
        if (!model.containsAttribute("sampleSummary")) {
            model.addAttribute("sampleSummary", new SummaryDisplayView(
                    new WebAnalysisResponse(
                            "sample-request",
                            "bookstore.zip",
                            "2026-04-10 15:00",
                            84,
                            6,
                            1,
                            2,
                            3,
                            new DownloadLinkView(message("result.download.human"), "#artifacts", "qscout-report.md"),
                            new DownloadLinkView(message("result.download.ai"), "#artifacts", "qscout-ai-input.md"),
                            "#artifacts",
                            "#artifacts",
                            message("page.home.sample.summary.message"),
                            message("page.home.sample.summary.autoExcluded"),
                            false,
                            true
                    ),
                    true,
                    "#artifacts"
            ));
        }
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }

    private String sanitizedFileName(MultipartFile file) {
        return file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
    }
}
