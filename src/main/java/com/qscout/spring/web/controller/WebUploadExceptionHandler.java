package com.qscout.spring.web.controller;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.dto.UploadErrorModalView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(annotations = Controller.class)
public class WebUploadExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebUploadExceptionHandler.class);

    private final MessageSource messageSource;

    public WebUploadExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handleMultipartException(Exception exception, RedirectAttributes redirectAttributes) {
        logger.warn("Multipart upload failed before controller completed.", exception);
        redirectAttributes.addFlashAttribute("uploadErrorModal", new UploadErrorModalView(
                message("error.upload.tooLarge.title"),
                message("error.upload.tooLarge.body"),
                message("error.upload.tooLarge.retry")
        ));
        return "redirect:/";
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }
}