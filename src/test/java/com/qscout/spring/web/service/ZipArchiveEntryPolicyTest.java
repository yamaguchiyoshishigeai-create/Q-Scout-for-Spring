package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipArchiveEntryPolicyTest {
    private final Function<String, String> messageResolver = Function.identity();

    @Test
    void marksConfiguredDirectoriesAsAutoExcluded() {
        ZipEntry entry = new ZipEntry("project/.git/objects/pack/pack-a.pack");

        ZipArchiveEntry result = ZipArchiveEntryPolicy.inspect(entry, messageResolver);

        assertThat(result.autoExcluded()).isTrue();
        assertThat(result.excludedDirectoryName()).isEqualTo(".git");
    }

    @Test
    void rejectsUnsafeCompressionRatioEvenForAutoExcludedEntry() {
        ZipEntry entry = new ZipEntry(".git/objects/pack/pack-a.pack");
        entry.setSize(10_000);
        entry.setCompressedSize(50);

        assertThatThrownBy(() -> ZipArchiveEntryPolicy.inspect(entry, messageResolver))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("error.invalidUpload.compressionRatio");
    }

    @Test
    void rejectsOversizedAutoExcludedEntryUsingDedicatedLimit() {
        ZipEntry entry = new ZipEntry(".git/objects/pack/pack-a.pack");
        entry.setSize(ZipSecurityLimits.MAX_SKIPPED_SINGLE_ENTRY_SIZE_BYTES + 1);

        assertThatThrownBy(() -> ZipArchiveEntryPolicy.inspect(entry, messageResolver))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("error.invalidUpload.skippedFileTooLarge");
    }
}
