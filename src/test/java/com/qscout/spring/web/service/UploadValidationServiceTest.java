package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadValidationServiceTest {
    private final UploadValidationService service = new UploadValidationService();

    @Test
    void rejectsNonZipFile() {
        MockMultipartFile file = new MockMultipartFile("projectZip", "sample.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("zipファイル");
    }

    @Test
    void acceptsZipWithEntries() {
        MockMultipartFile file = new MockMultipartFile("projectZip", "sample.zip", "application/zip", zipBytes("pom.xml", "<project/>") );

        assertThatCode(() -> service.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyZip() {
        MockMultipartFile file = new MockMultipartFile("projectZip", "empty.zip", "application/zip", emptyZipBytes());

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("空");
    }

    @Test
    void rejectsZipWithTooManyEntries() {
        MockMultipartFile file = new MockMultipartFile("projectZip", "many.zip", "application/zip", zipWithEntryCount(ZipSecurityLimits.MAX_ENTRY_COUNT + 1));

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("エントリ数");
    }

    @Test
    void rejectsZipWithDeepNesting() {
        String deepEntry = "a/".repeat(ZipSecurityLimits.MAX_ENTRY_DEPTH) + "pom.xml";
        MockMultipartFile file = new MockMultipartFile("projectZip", "deep.zip", "application/zip", zipBytes(deepEntry, "<project/>"));

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("階層");
    }

    @Test
    void acceptsZipWithLargeAutoExcludedEntry() {
        MockMultipartFile file = new MockMultipartFile(
                "projectZip",
                "auto-excluded.zip",
                "application/zip",
                zipBytes(orderedEntries(
                        ".git/objects/pack/pack-a.pack", "x".repeat(12 * 1024 * 1024),
                        "pom.xml", "<project/>"
                ))
        );

        assertThatCode(() -> service.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void rejectsTraversalEvenWhenPretendingToBeGit() {
        MockMultipartFile file = new MockMultipartFile(
                "projectZip",
                "bad-git.zip",
                "application/zip",
                zipBytes(orderedEntries(".git/../../evil.txt", "boom"))
        );

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("不正なパス");
    }

    @Test
    void rejectsNullCharacterInAutoExcludedEntry() {
        MockMultipartFile file = new MockMultipartFile(
                "projectZip",
                "bad-null.zip",
                "application/zip",
                zipBytes(orderedEntries(".git/\u0000/config", "boom"))
        );

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("不正なパス");
    }

    @Test
    void rejectsDeeplyNestedAutoExcludedEntry() {
        String deepEntry = ".git/" + "a/".repeat(ZipSecurityLimits.MAX_ENTRY_DEPTH) + "pack.idx";
        MockMultipartFile file = new MockMultipartFile(
                "projectZip",
                "deep-git.zip",
                "application/zip",
                zipBytes(orderedEntries(deepEntry, "content"))
        );

        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("階層");
    }

    private byte[] zipBytes(String entryName, String content) {
        return zipBytes(orderedEntries(entryName, content));
    }

    private byte[] zipBytes(Map<String, String> entries) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                    zipOutputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    zipOutputStream.closeEntry();
                }
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] emptyZipBytes() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream ignored = new ZipOutputStream(outputStream)) {
                // no entries
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] zipWithEntryCount(int count) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (int i = 0; i < count; i++) {
                    zipOutputStream.putNextEntry(new ZipEntry("src/main/java/File" + i + ".java"));
                    zipOutputStream.write("class A {}".getBytes());
                    zipOutputStream.closeEntry();
                }
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Map<String, String> orderedEntries(String firstName, String firstContent, String secondName, String secondContent) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(firstName, firstContent);
        entries.put(secondName, secondContent);
        return entries;
    }

    private Map<String, String> orderedEntries(String entryName, String content) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(entryName, content);
        return entries;
    }
}
