package com.qscout.spring.web.service;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.exception.InvalidUploadException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipInputStream;

@Service
public class UploadValidationService {
    public static final long MAX_UPLOAD_SIZE_BYTES = 20L * 1024 * 1024;

    private final MessageSource messageSource;

    public UploadValidationService() {
        this(MessageSources.create());
    }

    public UploadValidationService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException(message("error.invalidUpload.empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new InvalidUploadException(message("error.invalidUpload.extension"));
        }

        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new InvalidUploadException(message("error.invalidUpload.size"));
        }

        try (InputStream inputStream = file.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            if (zipInputStream.getNextEntry() == null) {
                throw new InvalidUploadException(message("error.invalidUpload.emptyArchive"));
            }
        } catch (IOException exception) {
            throw new InvalidUploadException(message("error.invalidUpload.unzip"), exception);
        }
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }
}
