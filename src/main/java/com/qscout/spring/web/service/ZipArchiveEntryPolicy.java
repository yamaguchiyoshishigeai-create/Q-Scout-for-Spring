package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidUploadException;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.zip.ZipEntry;

final class ZipArchiveEntryPolicy {
    private ZipArchiveEntryPolicy() {
    }

    static ZipArchiveEntry inspect(ZipEntry entry, Function<String, String> messageResolver) {
        String entryName = entry != null ? entry.getName() : null;
        if (entryName == null || entryName.isBlank()) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.path"));
        }

        String normalized = ZipSecurityLimits.normalizeEntryName(entryName);
        if (normalized.isBlank() || normalized.contains("\u0000")) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.path"));
        }
        if (normalized.contains(":")) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.invalidEntry"));
        }

        Path normalizedPath = Path.of(normalized).normalize();
        String normalizedPathText = normalizedPath.toString().replace('\\', '/');
        if (normalizedPath.isAbsolute() || normalizedPath.startsWith("..") || normalizedPathText.contains("../")) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.path"));
        }
        if (ZipSecurityLimits.pathDepth(normalized) > ZipSecurityLimits.MAX_ENTRY_DEPTH) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.nesting"));
        }

        String excludedDirectory = findExcludedDirectory(normalizedPath);
        validateSize(entry, excludedDirectory != null, messageResolver);
        validateCompressionRatio(entry, messageResolver);

        return new ZipArchiveEntry(normalized, entry.isDirectory(), excludedDirectory != null, excludedDirectory);
    }

    static void validateSkippedDeclaredTotal(long skippedDeclaredTotal, Function<String, String> messageResolver) {
        if (skippedDeclaredTotal > ZipSecurityLimits.MAX_SKIPPED_DECLARED_TOTAL_SIZE_BYTES) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.skippedTotalSize"));
        }
    }

    private static void validateSize(ZipEntry entry, boolean autoExcluded, Function<String, String> messageResolver) {
        long declaredSize = entry.getSize();
        long limit = autoExcluded
                ? ZipSecurityLimits.MAX_SKIPPED_SINGLE_ENTRY_SIZE_BYTES
                : ZipSecurityLimits.MAX_SINGLE_ENTRY_SIZE_BYTES;
        if (declaredSize > limit) {
            String key = autoExcluded
                    ? "error.invalidUpload.skippedFileTooLarge"
                    : "error.invalidUpload.fileTooLarge";
            throw new InvalidUploadException(messageResolver.apply(key));
        }
    }

    private static void validateCompressionRatio(ZipEntry entry, Function<String, String> messageResolver) {
        long declaredSize = entry.getSize();
        long compressedSize = entry.getCompressedSize();
        if (declaredSize > 0
                && compressedSize > 0
                && declaredSize / compressedSize > ZipSecurityLimits.MAX_COMPRESSION_RATIO) {
            throw new InvalidUploadException(messageResolver.apply("error.invalidUpload.compressionRatio"));
        }
    }

    private static String findExcludedDirectory(Path normalizedPath) {
        for (Path segment : normalizedPath) {
            String segmentName = segment.toString();
            if (ZipSecurityLimits.AUTO_EXCLUDED_DIRECTORY_NAMES.contains(segmentName)) {
                return segmentName;
            }
        }
        return null;
    }
}
