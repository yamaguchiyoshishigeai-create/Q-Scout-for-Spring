package com.qscout.spring.web;

import com.qscout.spring.web.service.UploadValidationService;
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
import static org.hamcrest.Matchers.not;
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Q-Scout for Spring</title>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("現在の対応範囲")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("プロジェクト zip をアップロード")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pom.xml を含む Spring Boot / Maven / 単一モジュールの zip を選択してください")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析を実行する")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析中です。しばらくお待ちください。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("日本語")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("English")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("アップロードに失敗しました")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("閉じる")))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Current Support Scope"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Run Analysis"))));
    }

    @Test
    void switchesToEnglishWithLangParameter() throws Exception {
        mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<html lang=\"en\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Q-Scout for Spring</title>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Current Support Scope")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload Project Zip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose a zip archive that contains a single-module Spring Boot / Maven project with pom.xml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Analysis is running. Please wait a moment.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("English")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload failed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Close")))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("現在の対応範囲"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("解析を実行する"))));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("アップロードに失敗しました")))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Current Support Scope"))));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload failed")));
    }

    @Test
    void showsLocalizedErrorMessagesForInvalidUpload() throws Exception {
        mockMvc.perform(multipart("/analyze"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("zipファイルを選択してください。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("内容を確認して再度アップロードしてください。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("閉じる")));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(multipart("/analyze").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please select a zip file.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please review the issue and upload the project again.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Close")));
    }

    @Test
    void showsLocalizedProjectStructureErrors() throws Exception {
        MockMultipartFile missingPomZip = new MockMultipartFile(
                "projectZip",
                "missing-pom.zip",
                "application/zip",
                zipDirectory(Path.of("samples/invalid-no-pom").toAbsolutePath().normalize())
        );

        mockMvc.perform(multipart("/analyze").file(missingPomZip))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pom.xml が見つかりません。Spring Boot / Mavenプロジェクトをアップロードしてください。")));

        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(multipart("/analyze").file(missingPomZip).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pom.xml was not found. Please upload a Spring Boot / Maven project.")));
    }

    @Test
    void showsUploadTooLargeModalInJapanese() throws Exception {
        mockMvc.perform(multipart("/analyze").file(oversizedZip()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("uploadErrorModal"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("アップロードに失敗しました")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("アップロードしたzipファイルが上限サイズ20MBを超えています。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("20MB以下のzipファイルを選択して、再度実行してください。")));
    }

    @Test
    void showsUploadTooLargeModalInEnglishAfterLocaleSwitch() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/").param("lang", "en"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(multipart("/analyze").file(oversizedZip()).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("uploadErrorModal"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload failed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("The uploaded zip file exceeds the 20MB limit.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please choose a zip file of 20MB or less and try again.")));
    }

    @Test
    void homePageIncludesClientSideUploadSizeGuard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-max-upload-bytes=\"20971520\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"uploadErrorModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("handleTooLargeFile")));
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
        assertThat(humanContent).contains("## このレポートの読み方");
        assertThat(humanContent).contains("## ルール別サマリ");
        assertThat(humanContent).contains("## 違反一覧");
        assertThat(humanContent).contains("## 改善ヒント");
        assertThat(humanContent).contains("総合スコア");

        MvcResult aiReport = mockMvc.perform(get("/download/{requestId}/ai", requestId))
                .andExpect(status().isOk())
                .andReturn();
        String aiContent = new String(aiReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(aiContent).contains("# Project Analysis Input");
        assertThat(aiContent).contains("## Detected Issues");
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
                .andReturn();

        Matcher matcher = DOWNLOAD_PATTERN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        String requestId = matcher.group(1);

        MvcResult humanReport = mockMvc.perform(get("/download/{requestId}/human", requestId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String humanContent = new String(humanReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(humanContent).contains("Q-Scout Report");
        assertThat(humanContent).contains("## How To Read This Report");
        assertThat(humanContent).contains("## Rule Summary");
        assertThat(humanContent).contains("## Violations");
        assertThat(humanContent).contains("## Improvement Hints");
        assertThat(humanContent).contains("Overall Score");
        assertThat(humanContent).doesNotContain("Q-Scout 診断レポート");

        MvcResult aiReport = mockMvc.perform(get("/download/{requestId}/ai", requestId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String aiContent = new String(aiReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(aiContent).contains("# Project Analysis Input");
        assertThat(aiContent).contains("## Detected Issues");
        assertThat(aiContent).contains("## Instructions");
        assertThat(aiContent).doesNotContain("Q-Scout Report");
        assertThat(aiContent).doesNotContain("Q-Scout 診断レポート");
    }

    @Test
    void rejectsInvalidFileKey() throws Exception {
        mockMvc.perform(get("/download/{requestId}/{fileKey}", "00000000-0000-0000-0000-000000000000", "bad"))
                .andExpect(status().isNotFound());
    }

    private MockMultipartFile oversizedZip() {
        return new MockMultipartFile(
                "projectZip",
                "too-large.zip",
                "application/zip",
                new byte[(int) UploadValidationService.MAX_UPLOAD_SIZE_BYTES + 1]
        );
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
