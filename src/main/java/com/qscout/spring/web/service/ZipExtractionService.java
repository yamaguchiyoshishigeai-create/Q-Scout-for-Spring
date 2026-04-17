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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    public ZipExtractionResult extract(Path zipPath, Path extractedDir) {
        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            int entryCount = 0;
            int extractedEntryCount = 0;
            int skippedEntryCount = 0;
            long totalExtractedBytes = 0;
            long skippedDeclaredTotal = 0;
            Set<String> skippedDirectories = new LinkedHashSet<>();
            while ((entry = zipInputStream.getNextEntry()) != null) {
                ZipArchiveEntry inspectedEntry = ZipArchiveEntryPolicy.inspect(entry, this::message);
                entryCount++;
                if (entryCount > ZipSecurityLimits.MAX_ENTRY_COUNT) {
                    throw new InvalidUploadException(message("error.invalidUpload.tooManyEntries"));
                }
                if (inspectedEntry.autoExcluded()) {
                    skippedEntryCount++;
                    skippedDirectories.add(inspectedEntry.excludedDirectoryName());
                    if (entry.getSize() > 0) {
                        skippedDeclaredTotal += entry.getSize();
                        ZipArchiveEntryPolicy.validateSkippedDeclaredTotal(skippedDeclaredTotal, this::message);
                    }
                    zipInputStream.closeEntry();
                    continue;
                }
                String entryName = inspectedEntry.normalizedEntryName();
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
                    extractedEntryCount++;
                }
                zipInputStream.closeEntry();
            }
            return new ZipExtractionResult(extractedEntryCount, skippedEntryCount, Set.copyOf(skippedDirectories));
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

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, MessageSources.resolveLocale());
    }
}
