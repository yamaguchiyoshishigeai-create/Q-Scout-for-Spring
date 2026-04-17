package com.qscout.spring.web.service;

import java.util.Set;

public record ZipExtractionResult(
        int extractedEntryCount,
        int skippedEntryCount,
        Set<String> skippedDirectoryNames
) {
    public boolean hasAutoExcludedEntries() {
        return skippedEntryCount > 0;
    }
}
