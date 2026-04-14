package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidProjectStructureException;
import com.qscout.spring.web.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
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
        Files.write(zipPath, zipBytes(Map.of("../evil.txt", "boom")));

        assertThatThrownBy(() -> service.extract(zipPath, extractedDir))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("不正なパス");
    }

    @Test
    void extractsArchiveWithBackslashEntryNames() throws IOException {
        Path zipPath = tempDir.resolve("windows-style.zip");
        Path extractedDir = tempDir.resolve("windows-style-out");
        Files.createDirectories(extractedDir);
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("sample\\pom.xml", "<project/>");
        entries.put("sample\\src\\main\\java\\App.java", "class App {}\n");
        Files.write(zipPath, zipBytes(entries));

        service.extract(zipPath, extractedDir);

        assertThat(Files.readString(extractedDir.resolve("sample/pom.xml"))).isEqualTo("<project/>");
        assertThat(Files.readString(extractedDir.resolve("sample/src/main/java/App.java"))).contains("class App");
        assertThat(service.resolveProjectRoot(extractedDir)).isEqualTo(extractedDir.resolve("sample").normalize());
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

    private byte[] zipBytes(Map<String, String> entries) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                    zipOutputStream.write(entry.getValue().getBytes());
                    zipOutputStream.closeEntry();
                }
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
