package com.qscout.spring.web.service;

import java.time.Duration;

public final class ZipSecurityLimits {
    public static final long MAX_UPLOAD_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_EXTRACTED_SIZE_BYTES = 100L * 1024 * 1024;
    public static final int MAX_ENTRY_COUNT = 5_000;
    public static final long MAX_SINGLE_ENTRY_SIZE_BYTES = 10L * 1024 * 1024;
    public static final int MAX_ENTRY_DEPTH = 20;
    public static final int MAX_COMPRESSION_RATIO = 100;
    public static final Duration WORKSPACE_RETENTION = Duration.ofMinutes(15);

    private ZipSecurityLimits() {
    }
}
