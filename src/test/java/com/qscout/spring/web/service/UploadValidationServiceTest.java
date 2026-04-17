package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
}
