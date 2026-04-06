package com.qscout.spring.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebFlowTest {
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("/download/([0-9a-fA-F-]{36})/human");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMissingUpload() throws Exception {
        mockMvc.perform(multipart("/analyze"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void analyzesZipAndAllowsDownloads() throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "projectZip",
                "sample-project.zip",
                "application/zip",
                zipDirectory(Path.of("src/test/resources/sample-project").toAbsolutePath().normalize())
        );

        MvcResult result = mockMvc.perform(multipart("/analyze").file(zip))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("総合スコア")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("人間向けMarkdown")))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        Matcher matcher = DOWNLOAD_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        String requestId = matcher.group(1);

        mockMvc.perform(get("/download/{requestId}/human", requestId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Q-Scout Report")));

        mockMvc.perform(get("/download/{requestId}/ai", requestId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Project Analysis Input")));
    }

    @Test
    void rejectsInvalidFileKey() throws Exception {
        mockMvc.perform(get("/download/{requestId}/{fileKey}", "00000000-0000-0000-0000-000000000000", "bad"))
                .andExpect(status().isNotFound());
    }

    private byte[] zipDirectory(Path root) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                 var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String entryName = root.relativize(path).toString().replace('\\', '/');
                    try {
                        zipOutputStream.putNextEntry(new ZipEntry(entryName));
                        zipOutputStream.write(Files.readAllBytes(path));
                        zipOutputStream.closeEntry();
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
