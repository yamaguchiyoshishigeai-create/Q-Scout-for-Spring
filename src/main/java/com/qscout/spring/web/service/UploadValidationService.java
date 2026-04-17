package com.qscout.spring.web.service;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.exception.UploadTooLargeException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class UploadValidationService {
    public static final long MAX_UPLOAD_SIZE_BYTES = ZipSecurityLimits.MAX_UPLOAD_SIZE_BYTES;

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
            throw new UploadTooLargeException(message("error.invalidUpload.size"));
        }

        try (InputStream inputStream = file.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            boolean hasEntries = false;
            int entryCount = 0;
            long skippedDeclaredTotal = 0;
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                hasEntries = true;
                entryCount++;
                if (entryCount > ZipSecurityLimits.MAX_ENTRY_COUNT) {
                    throw new InvalidUploadException(message("error.invalidUpload.tooManyEntries"));
                }
                ZipArchiveEntry inspectedEntry = ZipArchiveEntryPolicy.inspect(entry, this::message);
                if (inspectedEntry.autoExcluded() && entry.getSize() > 0) {
                    skippedDeclaredTotal += entry.getSize();
                    ZipArchiveEntryPolicy.validateSkippedDeclaredTotal(skippedDeclaredTotal, this::message);
                }
                zipInputStream.closeEntry();
            }
            if (!hasEntries) {
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
