package com.qscout.spring.web.service;

import java.time.Duration;
import java.util.Set;

public final class ZipSecurityLimits {
    public static final long MAX_UPLOAD_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_EXTRACTED_SIZE_BYTES = 100L * 1024 * 1024;
    public static final int MAX_ENTRY_COUNT = 5_000;
    public static final long MAX_SINGLE_ENTRY_SIZE_BYTES = 10L * 1024 * 1024;
    public static final long MAX_SKIPPED_SINGLE_ENTRY_SIZE_BYTES = 64L * 1024 * 1024;
    public static final long MAX_SKIPPED_DECLARED_TOTAL_SIZE_BYTES = 250L * 1024 * 1024;
    public static final int MAX_ENTRY_DEPTH = 20;
    public static final int MAX_COMPRESSION_RATIO = 100;
    public static final Duration WORKSPACE_RETENTION = Duration.ofMinutes(15);
    public static final Set<String> AUTO_EXCLUDED_DIRECTORY_NAMES = Set.of(
            ".git",
            ".github",
            "target",
            "build",
            "node_modules",
            ".idea",
            ".vscode"
    );

    private ZipSecurityLimits() {
    }

    public static String normalizeEntryName(String entryName) {
        if (entryName == null) {
            return "";
        }
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public static int pathDepth(String normalizedEntryName) {
        String trimmed = normalizedEntryName.endsWith("/")
                ? normalizedEntryName.substring(0, normalizedEntryName.length() - 1)
                : normalizedEntryName;
        if (trimmed.isBlank()) {
            return 0;
        }
        return trimmed.split("/").length;
    }
}
