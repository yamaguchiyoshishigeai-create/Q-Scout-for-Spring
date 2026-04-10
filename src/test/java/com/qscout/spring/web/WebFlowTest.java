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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"ja\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("現在の対応範囲")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析を実行する")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("日本語")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("English")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Current Support Scope"))));
    }

    @Test
    void switchesToEnglishWithLangParameter() throws Exception {
        mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"en\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Support Scope")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("English")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("現在の対応範囲"))));
    }

    @Test
    void switchesBackToJapaneseWithLangParameter() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/").param("lang", "ja").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"ja\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("現在の対応範囲")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析を実行する")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Current Support Scope"))));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"en\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Support Scope")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")));
    }

    @Test
    void showsLocalizedErrorMessagesForInvalidUpload() throws Exception {
        mockMvc.perform(multipart("/analyze"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("zipファイルを選択してください。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("内容を確認して再度アップロードしてください。")));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(multipart("/analyze").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please select a zip file.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please review the issue and upload the project again.")));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析が完了しました。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("総合スコア")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("違反件数")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("人間向けMarkdown")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI入力Markdown")))
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
        assertThat(humanContent).contains("総合スコア");

        MvcResult aiReport = mockMvc.perform(get("/download/{requestId}/ai", requestId))
                .andExpect(status().isOk())
                .andReturn();
        String aiContent = new String(aiReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(aiContent).contains("# Project Analysis Input");
        assertThat(aiContent).contains("## Instructions");
        assertThat(aiContent).doesNotContain("# Q-Scout 診断レポート");
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Analysis completed.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Overall Score")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Violations")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Human-readable Markdown")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI input Markdown")))
                .andReturn();

        Matcher matcher = DOWNLOAD_PATTERN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        String requestId = matcher.group(1);

        MvcResult humanReport = mockMvc.perform(get("/download/{requestId}/human", requestId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String humanContent = new String(humanReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(humanContent).contains("Q-Scout Report");
        assertThat(humanContent).contains("Overall Score");
        assertThat(humanContent).doesNotContain("Q-Scout 診断レポート");

        MvcResult aiReport = mockMvc.perform(get("/download/{requestId}/ai", requestId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String aiContent = new String(aiReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(aiContent).contains("# Project Analysis Input");
        assertThat(aiContent).contains("## Instructions");
        assertThat(aiContent).doesNotContain("Q-Scout Report");
        assertThat(aiContent).doesNotContain("Q-Scout 診断レポート");
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
