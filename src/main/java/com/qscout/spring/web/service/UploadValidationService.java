package com.qscout.spring.web.service;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.exception.InvalidUploadException;
import com.qscout.spring.web.exception.UploadTooLargeException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                hasEntries = true;
                entryCount++;
                if (entryCount > ZipSecurityLimits.MAX_ENTRY_COUNT) {
                    throw new InvalidUploadException(message("error.invalidUpload.tooManyEntries"));
                }
                validateEntryMetadata(entry);
                zipInputStream.closeEntry();
            }
            if (!hasEntries) {
                throw new InvalidUploadException(message("error.invalidUpload.emptyArchive"));
            }
        } catch (IOException exception) {
            throw new InvalidUploadException(message("error.invalidUpload.unzip"), exception);
        }
    }

    private void validateEntryMetadata(ZipEntry entry) {
        String entryName = entry.getName();
        if (entryName == null || entryName.isBlank()) {
            throw new InvalidUploadException(message("error.invalidUpload.path"));
        }
        String normalized = normalizeEntryName(entryName);
        if (normalized.isBlank() || normalized.contains("\u0000")) {
            throw new InvalidUploadException(message("error.invalidUpload.path"));
        }
        if (normalized.contains(":")) {
            throw new InvalidUploadException(message("error.invalidUpload.invalidEntry"));
        }
        Path normalizedPath = Path.of(normalized).normalize();
        if (normalizedPath.isAbsolute() || normalizedPath.startsWith("..")) {
            throw new InvalidUploadException(message("error.invalidUpload.path"));
        }
        if (pathDepth(normalized) > ZipSecurityLimits.MAX_ENTRY_DEPTH) {
            throw new InvalidUploadException(message("error.invalidUpload.nesting"));
        }
        long declaredSize = entry.getSize();
        if (declaredSize > ZipSecurityLimits.MAX_SINGLE_ENTRY_SIZE_BYTES) {
            throw new InvalidUploadException(message("error.invalidUpload.fileTooLarge"));
        }
        long compressedSize = entry.getCompressedSize();
        if (declaredSize > 0
                && compressedSize > 0
                && declaredSize / compressedSize > ZipSecurityLimits.MAX_COMPRESSION_RATIO) {
            throw new InvalidUploadException(message("error.invalidUpload.compressionRatio"));
        }
    }

    private String normalizeEntryName(String entryName) {
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private int pathDepth(String normalizedEntryName) {
        String trimmed = normalizedEntryName.endsWith("/") ? normalizedEntryName.substring(0, normalizedEntryName.length() - 1) : normalizedEntryName;
        if (trimmed.isBlank()) {
            return 0;
        }
        return trimmed.split("/").length;
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }
}
