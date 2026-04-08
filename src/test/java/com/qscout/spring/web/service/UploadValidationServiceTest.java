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
