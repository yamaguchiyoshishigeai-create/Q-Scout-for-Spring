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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("はじめに")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("診断を実行")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("このシステムは、Spring Boot / Maven プロジェクトの品質診断システムです。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Web版では、20MB以下のZIPファイルをアップロードして診断を実行できます。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析結果は、ユーザー向けレポートとAI向け入力の2種類で確認・ダウンロードできます。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pom.xml を含む Spring Boot / Maven / 単一モジュールの zip を選択してください")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ZIP 作成の目安")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("プロジェクト直下に pom.xml が見える形で ZIP 化してください")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析を実行する")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析中です。しばらくお待ちください。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("成果物サンプル")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("サンプル結果を表示しています。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bookstore.zip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">84/100<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">6<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("成果物の種類を見る")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("使い方・仕様")))
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Getting Started")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Diagnosis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("This system analyzes the quality of Spring Boot / Maven projects.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("In the web app, you can upload a ZIP file up to 20MB and run the diagnosis.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("The output is available in two forms: a human-readable report and AI-ready input.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose a zip archive that contains a single-module Spring Boot / Maven project with pom.xml")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ZIP guidance")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create the ZIP so pom.xml is visible at the project root")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Analysis is running. Please wait a moment.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Artifact Sample")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Showing a sample result.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bookstore.zip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">84/100<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">6<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("See artifact types")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usage &amp; Specs")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("English")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload failed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Close")))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("現在の対応範囲"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("解析を実行する"))));
    }

    @Test
    void helpPageIsLocalized() throws Exception {
        mockMvc.perform(get("/help"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("使い方・仕様")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("利用の流れ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("推奨 ZIP 作成方法")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ZIP を開いた最上位で pom.xml が確認できる単一モジュール構成を推奨します。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析画面へ戻る")));

        mockMvc.perform(get("/help").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usage &amp; Specs")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("How It Works")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recommended ZIP Packaging")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Prefer a single-module archive where pom.xml is visible at the top level when the ZIP is opened.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Back to Analyzer")));
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("はじめに")))
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Getting Started")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Run Analysis")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Artifact Sample")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Showing a sample result.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bookstore.zip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usage &amp; Specs")))
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
    void analyzesZipAndShowsReadableJapaneseSummary() throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "projectZip",
                "sample-project.zip",
                "application/zip",
                zipDirectory(Path.of("samples/sample-project").toAbsolutePath().normalize())
        );

        MvcResult result = mockMvc.perform(multipart("/analyze").file(zip))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("解析が完了しました。")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("対象ファイル：")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sample-project.zip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("実行時刻：")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("総合スコア")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">63/100<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("100点満点")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("違反件数")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("内訳：")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("高")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("中")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("低")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ユーザー向けMarkdown")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI入力Markdown")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("プレビュー")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ダウンロード")))
                .andReturn();

        String requestId = extractRequestId(result);
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("/preview/" + requestId + "/human?lang=ja");
        assertThat(html).contains("/preview/" + requestId + "/ai?lang=ja");
        assertThat(html).contains("/download/" + requestId + "/human");
        assertThat(html).contains("/download/" + requestId + "/ai");

        MvcResult humanReport = mockMvc.perform(get("/download/{requestId}/human", requestId))
                .andExpect(status().isOk())
                .andReturn();
        String humanContent = new String(humanReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(humanContent).contains("Q-Scout 診断レポート");
        assertThat(humanContent).contains("## このレポートの読み方");
        assertThat(humanContent).contains("## ルール別サマリ");
        assertThat(humanContent).contains("## 違反一覧");
        assertThat(humanContent).contains("## 今回の検査対象ルール");
        assertThat(humanContent).contains("## 改善ヒント");
        assertThat(humanContent).contains("総合スコア");
        assertThat(humanContent).contains("[詳細解説を見る](/help/rules/controller-to-repository-direct-access?lang=ja)");
        assertThat(humanContent).doesNotContain("詳細解説キー:");

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
    void previewPagesAreAvailableAndIncludeDownloadLinks() throws Exception {
        MockMultipartFile zip = new MockMultipartFile(
                "projectZip",
                "sample-project.zip",
                "application/zip",
                zipDirectory(Path.of("samples/sample-project").toAbsolutePath().normalize())
        );

        MvcResult analyzeResult = mockMvc.perform(multipart("/analyze").file(zip))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = extractRequestId(analyzeResult);

        mockMvc.perform(get("/preview/{requestId}/human", requestId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ユーザー向けMarkdownプレビュー")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("qscout-report.md")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("このMarkdownをダウンロード")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/download/" + requestId + "/human")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Q-Scout 診断レポート")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/help/rules/controller-to-repository-direct-access?lang=ja\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/help/rules/exception-swallowing?lang=ja\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("今回の検査対象ルール")))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("詳細解説キー:"))));

        mockMvc.perform(get("/preview/{requestId}/ai", requestId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI入力Markdownプレビュー")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("qscout-ai-input.md")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("このMarkdownをダウンロード")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/download/" + requestId + "/ai")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# Project Analysis Input")));
    }

    @Test
    void generatesEnglishReportAndReadableSummaryAfterLocaleSwitch() throws Exception {
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Target file:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Executed at:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Artifact Sample")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Showing a sample result.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("See artifact types")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usage &amp; Specs")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Overall Score")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">63/100<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Out of 100")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Violations")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Breakdown:")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("High")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Medium")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Low")))
                .andReturn();

        String requestId = extractRequestId(result);

        MvcResult humanReport = mockMvc.perform(get("/download/{requestId}/human", requestId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        String humanContent = new String(humanReport.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(humanContent).contains("Q-Scout Report");
        assertThat(humanContent).contains("## How To Read This Report");
        assertThat(humanContent).contains("## Rule Summary");
        assertThat(humanContent).contains("## Violations");
        assertThat(humanContent).contains("## Checked Rules");
        assertThat(humanContent).contains("## Improvement Hints");
        assertThat(humanContent).contains("Overall Score");
        assertThat(humanContent).contains("[View detailed explanation](/help/rules/controller-to-repository-direct-access?lang=en)");
        assertThat(humanContent).doesNotContain("Detail key:");
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

        mockMvc.perform(get("/preview/{requestId}/human", requestId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Human-readable Markdown Preview")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Download This Markdown")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/help/rules/controller-to-repository-direct-access?lang=en\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Checked Rules")))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Detail key:"))));
    }


    @Test
    void ruleHelpPagesAreLocalizedAndResolvable() throws Exception {
        mockMvc.perform(get("/help/rules/controller-to-repository-direct-access"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Controller から Repository への直接アクセス")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("このルールが検出すること")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Q-Scout が重視する理由")));

        mockMvc.perform(get("/help/rules/controller-to-repository-direct-access").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Controller To Repository Direct Access")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("What it detects")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Why Q-Scout cares")));

        mockMvc.perform(get("/help/rules/not-a-rule"))
                .andExpect(status().isNotFound());
    }
    @Test
    void rejectsInvalidFileKey() throws Exception {
        mockMvc.perform(get("/download/{requestId}/{fileKey}", "00000000-0000-0000-0000-000000000000", "bad"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/preview/{requestId}/{fileKey}", "00000000-0000-0000-0000-000000000000", "bad"))
                .andExpect(status().isNotFound());
    }

    private String extractRequestId(MvcResult result) throws Exception {
        Matcher matcher = DOWNLOAD_PATTERN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
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








