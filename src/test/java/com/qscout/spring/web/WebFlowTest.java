package com.qscout.spring.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
    void defaultsToJapaneseOnHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("現在の対応範囲")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析を実行する")));
    }

    @Test
    void switchesToEnglishWithLangParameter() throws Exception {
        mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Support Scope")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")));
    }

    @Test
    void keepsSelectedLocaleInSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Support Scope")));
    }

    @Test
    void rejectsMissingUpload() throws Exception {
        mockMvc.perform(multipart("/analyze"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void analyzesZipAndAllowsDownloadsInJapaneseByDefault() throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "projectZip",
                "sample-project.zip",
                "application/zip",
                zipDirectory(Path.of("samples/sample-project").toAbsolutePath().normalize())
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

        MvcResult humanReport = mockMvc.perform(get("/download/{requestId}/human", requestId))
                .andExpect(status().isOk())
                .andReturn();
        String humanContent = new String(humanReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(humanContent).contains("Q-Scout 診断レポート");

        mockMvc.perform(get("/download/{requestId}/ai", requestId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Project Analysis Input")));
    }

    @Test
    void generatesEnglishReportAfterLocaleSwitch() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andReturn()
                .getRequest()
                .getSession(false);

        MockMultipartFile zip = new MockMultipartFile(
                "projectZip",
                "sample-project.zip",
                "application/zip",
                zipDirectory(Path.of("samples/sample-project").toAbsolutePath().normalize())
        );

        MvcResult result = mockMvc.perform(multipart("/analyze").file(zip).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Overall Score")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Human-readable Markdown")))
                .andReturn();

        Matcher matcher = DOWNLOAD_PATTERN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        String requestId = matcher.group(1);

        MvcResult humanReport = mockMvc.perform(get("/download/{requestId}/human", requestId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String humanContent = new String(humanReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(humanContent).contains("Q-Scout Report");
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
