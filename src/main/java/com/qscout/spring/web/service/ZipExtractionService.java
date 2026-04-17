package com.qscout.spring.web.service;

import com.qscout.spring.i18n.MessageSources;
import com.qscout.spring.web.exception.InvalidProjectStructureException;
import com.qscout.spring.web.exception.InvalidUploadException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipExtractionService {
    private final MessageSource messageSource;

    public ZipExtractionService() {
        this(MessageSources.create());
    }

    public ZipExtractionService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public void saveUpload(MultipartFile file, Path uploadZipPath) {
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, uploadZipPath);
        } catch (IOException exception) {
            throw new InvalidUploadException(message("error.invalidUpload.save"), exception);
        }
    }

    public void extract(Path zipPath, Path extractedDir) {
        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            int entryCount = 0;
            long totalExtractedBytes = 0;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = normalizeEntryName(entry.getName());
                validateEntry(entry, entryName);
                entryCount++;
                if (entryCount > ZipSecurityLimits.MAX_ENTRY_COUNT) {
                    throw new InvalidUploadException(message("error.invalidUpload.tooManyEntries"));
                }
                Path target = extractedDir.resolve(entryName).normalize();
                if (!target.startsWith(extractedDir.normalize())) {
                    throw new InvalidUploadException(message("error.invalidUpload.path"));
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    long extractedSize = copyEntry(zipInputStream, target);
                    if (extractedSize > ZipSecurityLimits.MAX_SINGLE_ENTRY_SIZE_BYTES) {
                        throw new InvalidUploadException(message("error.invalidUpload.fileTooLarge"));
                    }
                    totalExtractedBytes += extractedSize;
                    if (totalExtractedBytes > ZipSecurityLimits.MAX_EXTRACTED_SIZE_BYTES) {
                        throw new InvalidUploadException(message("error.invalidUpload.totalSize"));
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException exception) {
            throw new InvalidUploadException(message("error.invalidUpload.unzip"), exception);
        }
    }

    public Path resolveProjectRoot(Path extractedDir) {
        Path directPom = extractedDir.resolve("pom.xml");
        if (Files.exists(directPom)) {
            return extractedDir;
        }

        List<Path> candidates = new ArrayList<>();
        try (var stream = Files.list(extractedDir)) {
            stream.filter(Files::isDirectory)
                    .map(path -> path.resolve("pom.xml"))
                    .filter(Files::exists)
                    .map(Path::getParent)
                    .forEach(candidates::add);
        } catch (IOException exception) {
            throw new InvalidProjectStructureException(message("error.projectRoot.resolve"));
        }

        if (candidates.isEmpty()) {
            throw new InvalidProjectStructureException(message("error.projectRoot.missingPom"));
        }
        if (candidates.size() > 1) {
            throw new InvalidProjectStructureException(message("error.projectRoot.multiplePom"));
        }
        return candidates.get(0).normalize();
    }

    private String normalizeEntryName(String entryName) {
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void validateEntry(ZipEntry entry, String entryName) {
        if (entryName.isBlank() || entryName.contains("\u0000")) {
            throw new InvalidUploadException(message("error.invalidUpload.path"));
        }
        if (entryName.contains(":")) {
            throw new InvalidUploadException(message("error.invalidUpload.invalidEntry"));
        }
        Path normalizedPath = Path.of(entryName).normalize();
        if (normalizedPath.isAbsolute() || normalizedPath.startsWith("..")) {
            throw new InvalidUploadException(message("error.invalidUpload.path"));
        }
        if (pathDepth(entryName) > ZipSecurityLimits.MAX_ENTRY_DEPTH) {
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

    private long copyEntry(InputStream inputStream, Path target) throws IOException {
        byte[] buffer = new byte[8192];
        long totalWritten = 0;
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                totalWritten += read;
                if (totalWritten > ZipSecurityLimits.MAX_SINGLE_ENTRY_SIZE_BYTES) {
                    throw new InvalidUploadException(message("error.invalidUpload.fileTooLarge"));
                }
                outputStream.write(buffer, 0, read);
            }
        } catch (RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return totalWritten;
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
