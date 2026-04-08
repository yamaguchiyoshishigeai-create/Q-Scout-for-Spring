package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidProjectStructureException;
import com.qscout.spring.web.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipExtractionServiceTest {
    private final ZipExtractionService service = new ZipExtractionService();

    @TempDir
    Path tempDir;

    @Test
    void resolvesSingleNestedProjectRoot() throws IOException {
        Path extractedDir = tempDir.resolve("extracted");
        Files.createDirectories(extractedDir.resolve("sample"));
        Files.writeString(extractedDir.resolve("sample/pom.xml"), "<project/>");

        assertThat(service.resolveProjectRoot(extractedDir)).isEqualTo(extractedDir.resolve("sample").normalize());
    }

    @Test
    void rejectsZipSlipEntry() throws IOException {
        Path zipPath = tempDir.resolve("bad.zip");
        Path extractedDir = tempDir.resolve("out");
        Files.createDirectories(extractedDir);
        Files.write(zipPath, zipBytes("../evil.txt", "boom"));

        assertThatThrownBy(() -> service.extract(zipPath, extractedDir))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("不正なパス");
    }

    @Test
    void rejectsMultipleProjectCandidates() throws IOException {
        Path extractedDir = tempDir.resolve("multi");
        Files.createDirectories(extractedDir.resolve("a"));
        Files.createDirectories(extractedDir.resolve("b"));
        Files.writeString(extractedDir.resolve("a/pom.xml"), "<project/>");
        Files.writeString(extractedDir.resolve("b/pom.xml"), "<project/>");

        assertThatThrownBy(() -> service.resolveProjectRoot(extractedDir))
                .isInstanceOf(InvalidProjectStructureException.class)
                .hasMessageContaining("複数");
    }

    private byte[] zipBytes(String entryName, String content) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                zipOutputStream.write(content.getBytes());
                zipOutputStream.closeEntry();
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
