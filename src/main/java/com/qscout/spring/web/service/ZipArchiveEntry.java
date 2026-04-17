package com.qscout.spring.web.service;

record ZipArchiveEntry(
        String normalizedEntryName,
        boolean directory,
        boolean autoExcluded,
        String excludedDirectoryName
) {
}
