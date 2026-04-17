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
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class WebPageController {
    private static final Logger logger = LoggerFactory.getLogger(WebPageController.class);

    private final WebAnalysisService webAnalysisService;
    private final RequestRateLimiter requestRateLimiter;
    private final MessageSource messageSource;

    public WebPageController(WebAnalysisService webAnalysisService, RequestRateLimiter requestRateLimiter, MessageSource messageSource) {
        this.webAnalysisService = webAnalysisService;
        this.requestRateLimiter = requestRateLimiter;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public String showIndex(Model model) {
        populateCommon(model);
        return "index";
    }

    @GetMapping("/help")
    public String showHelp(Model model) {
        populateCommon(model);
        return "help";
    }

    @PostMapping("/analyze")
    public String analyze(
            @RequestParam(value = "projectZip", required = false) MultipartFile file,
            Model model,
            HttpServletRequest request,
            HttpServletResponse servletResponse
    ) {
        populateCommon(model);
        RateLimitDecision decision = requestRateLimiter.evaluate(resolveClientIp(request));
        if (!decision.allowed()) {
            logger.warn("Analyze request rate limited. clientIp={}, reasonCode=RATE_LIMIT_EXCEEDED", resolveClientIp(request));
            servletResponse.setStatus(429);
            servletResponse.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            model.addAttribute("error", new ErrorViewModel(message("error.rateLimit.exceeded"), "RATE_LIMIT_EXCEEDED", true));
            return "index";
        }
        try {
            WebAnalysisResponse analysisResponse = webAnalysisService.analyze(file);
            model.addAttribute("response", analysisResponse);
            model.addAttribute("resultSummary", new SummaryDisplayView(analysisResponse, false, "#artifacts"));
        } catch (UploadTooLargeException exception) {
            logger.warn("Upload exceeded size limit during controller validation. filename={}, reasonCode=UPLOAD_TOO_LARGE",
                    sanitizedFileName(file));
            model.addAttribute("uploadErrorModal", new UploadErrorModalView(
                    message("error.upload.tooLarge.title"),
                    message("error.upload.tooLarge.body"),
                    message("error.upload.tooLarge.retry")
            ));
        } catch (InvalidUploadException | InvalidProjectStructureException exception) {
            logger.warn("Input validation failed during web analysis. filename={}, reasonCode=INPUT_ERROR",
                    sanitizedFileName(file));
            model.addAttribute("error", new ErrorViewModel(exception.getMessage(), "INPUT_ERROR", true));
        } catch (AnalysisTimeoutException exception) {
            logger.warn("Web analysis timed out. filename={}, reasonCode=TIMEOUT", sanitizedFileName(file));
            model.addAttribute("error", new ErrorViewModel(exception.getMessage(), "TIMEOUT", true));
        } catch (RuntimeException exception) {
            logger.error("Unexpected web analysis error.", exception);
            model.addAttribute("error", new ErrorViewModel(message("error.unexpected"), "UNEXPECTED", true));
        }
        return "index";
    }

    private void populateCommon(Model model) {
        model.addAttribute("limits", new ExecutionLimitView(20, 60, "zip"));
        model.addAttribute("resultSummary", null);
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

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private String sanitizedFileName(MultipartFile file) {
        return file != null && file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
    }
}
