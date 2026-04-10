package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.ErrorViewModel;
import com.qscout.spring.web.dto.ExecutionLimitView;
import com.qscout.spring.web.dto.UploadErrorModalView;
import com.qscout.spring.web.exception.AnalysisTimeoutException;
import com.qscout.spring.web.exception.InvalidProjectStructureException;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.exception.UploadTooLargeException;
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

@Controller
public class WebPageController {
    private static final Logger logger = LoggerFactory.getLogger(WebPageController.class);

    private final WebAnalysisService webAnalysisService;
    private final MessageSource messageSource;

    public WebPageController(WebAnalysisService webAnalysisService, MessageSource messageSource) {
        this.webAnalysisService = webAnalysisService;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public String showIndex(Model model) {
        populateCommon(model);
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam(value = "projectZip", required = false) MultipartFile file, Model model) {
        populateCommon(model);
        try {
            model.addAttribute("response", webAnalysisService.analyze(file));
        } catch (UploadTooLargeException exception) {
            logger.warn("Upload exceeded size limit during controller validation. filename={}", file != null ? file.getOriginalFilename() : null, exception);
            model.addAttribute("uploadErrorModal", new UploadErrorModalView(
                    message("error.upload.tooLarge.title"),
                    message("error.upload.tooLarge.body"),
                    message("error.upload.tooLarge.retry")
            ));
        } catch (InvalidUploadException | InvalidProjectStructureException exception) {
            logger.warn("Input validation failed during web analysis. filename={}", file != null ? file.getOriginalFilename() : null, exception);
            model.addAttribute("error", new ErrorViewModel(exception.getMessage(), "INPUT_ERROR", true));
        } catch (AnalysisTimeoutException exception) {
            logger.warn("Web analysis timed out. filename={}", file != null ? file.getOriginalFilename() : null, exception);
            model.addAttribute("error", new ErrorViewModel(exception.getMessage(), "TIMEOUT", true));
        } catch (RuntimeException exception) {
            logger.error("Unexpected web analysis error.", exception);
            model.addAttribute("error", new ErrorViewModel(message("error.unexpected"), "UNEXPECTED", true));
        }
        return "index";
    }

    private void populateCommon(Model model) {
        model.addAttribute("limits", new ExecutionLimitView(20, 60, "zip"));
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }
}