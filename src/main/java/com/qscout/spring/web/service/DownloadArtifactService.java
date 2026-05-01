package com.qscout.spring.web.service;

import com.qscout.spring.application.SharedAnalysisService;
import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * 生成済み成果物をダウンロード用またはプレビュー用に解決するサービスである。
 *
 * <p>requestId と fileKey に対応する成果物ファイルを一時ワークスペースから探し、
 * 参照形式に応じた戻り値へ整形する。</p>
 *
 * <p>解析本体や成果物生成は担わず、生成済みファイルの安全な参照導線に責務を留める。</p>
 */
@Service
public class DownloadArtifactService {
    private static final Map<String, ArtifactDefinition> FILE_MAPPING = Map.of(
            "human", new ArtifactDefinition(SharedAnalysisService.HUMAN_REPORT_FILE_NAME, MediaType.TEXT_MARKDOWN),
            "ai", new ArtifactDefinition("qscout-ai-input.md", MediaType.TEXT_MARKDOWN)
    );

    private final TempWorkspaceService tempWorkspaceService;

    public DownloadArtifactService(TempWorkspaceService tempWorkspaceService) {
        this.tempWorkspaceService = tempWorkspaceService;
    }

    /**
     * ダウンロード応答で返す成果物ファイル情報を解決する。
     *
     * @param requestId 成果物保持先を識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @return ダウンロード応答に必要なリソース情報
     * @throws ArtifactExpiredException 成果物保持期限が切れている場合
     * @throws IllegalArgumentException fileKey または参照パスが不正な場合
     */
    public DownloadArtifact resolveForDownload(String requestId, String fileKey) {
        return resolveForDownload(requestId, fileKey, null);
    }

    /**
     * ダウンロード応答で返す成果物ファイル情報を、必要に応じて表示言語に合わせて解決する。
     *
     * @param requestId 成果物保持先を識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @param language 表示言語。human 成果物の場合のみ ja / en を反映する。
     * @return ダウンロード応答に必要なリソース情報
     */
    public DownloadArtifact resolveForDownload(String requestId, String fileKey, String language) {
        ResolvedArtifact artifact = resolveArtifact(requestId, fileKey, language);
        Resource resource = new FileSystemResource(artifact.path());
        return new DownloadArtifact(resource, artifact.fileName(), artifact.contentType());
    }

    /**
     * プレビュー表示で使う成果物内容を文字列として解決する。
     *
     * @param requestId 成果物保持先を識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @return プレビュー画面で利用する成果物情報
     * @throws ArtifactExpiredException 成果物保持期限が切れている場合
     * @throws IllegalArgumentException fileKey または参照パスが不正な場合
     */
    public PreviewArtifact resolveForPreview(String requestId, String fileKey) {
        return resolveForPreview(requestId, fileKey, null);
    }

    /**
     * プレビュー表示で使う成果物内容を、必要に応じて表示言語に合わせて文字列として解決する。
     *
     * @param requestId 成果物保持先を識別する requestId
     * @param fileKey 参照対象成果物を識別するキー
     * @param language 表示言語。human 成果物の場合のみ ja / en を反映する。
     * @return プレビュー画面で利用する成果物情報
     */
    public PreviewArtifact resolveForPreview(String requestId, String fileKey, String language) {
        ResolvedArtifact artifact = resolveArtifact(requestId, fileKey, language);
        try {
            String content = Files.readString(artifact.path(), StandardCharsets.UTF_8);
            return new PreviewArtifact(
                    artifact.fileKey(),
                    artifact.fileName(),
                    artifact.contentType(),
                    content,
                    tempWorkspaceService.readScoreBandClass(requestId)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read preview artifact.", exception);
        }
    }

    private ResolvedArtifact resolveArtifact(String requestId, String fileKey) {
        return resolveArtifact(requestId, fileKey, null);
    }

    private ResolvedArtifact resolveArtifact(String requestId, String fileKey, String language) {
        ArtifactDefinition definition = FILE_MAPPING.get(fileKey);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid download file key.");
        }

        tempWorkspaceService.assertActive(requestId);
        Path rootDir = tempWorkspaceService.resolveWorkspaceRoot(requestId);
        String fileName = localizedFileName(fileKey, definition.fileName(), language);
        Path artifactPath = rootDir.resolve("output").resolve(fileName).normalize();
        if (!artifactPath.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid download path.");
        }
        if (!Files.exists(artifactPath) || !Files.isRegularFile(artifactPath)) {
            throw new ArtifactExpiredException("ダウンロード期限が切れました。再度解析を実行してください。");
        }
        return new ResolvedArtifact(fileKey, artifactPath, fileName, definition.contentType());
    }

    private String localizedFileName(String fileKey, String defaultFileName, String language) {
        if (!"human".equals(fileKey)) {
            return defaultFileName;
        }
        String normalizedLanguage = language == null ? "" : Locale.forLanguageTag(language).getLanguage();
        return switch (normalizedLanguage) {
            case "ja" -> SharedAnalysisService.HUMAN_REPORT_JA_FILE_NAME;
            case "en" -> SharedAnalysisService.HUMAN_REPORT_EN_FILE_NAME;
            default -> defaultFileName;
        };
    }

    private record ArtifactDefinition(
            String fileName,
            MediaType contentType
    ) {
    }

    private record ResolvedArtifact(
            String fileKey,
            Path path,
            String fileName,
            MediaType contentType
    ) {
    }

    /**
     * ダウンロード応答で返す成果物リソース情報をまとめる値オブジェクトである。
     *
     * @param resource ダウンロード対象リソース
     * @param fileName ダウンロード時に提示するファイル名
     * @param contentType HTTP 応答で返す Content-Type
     */
    public record DownloadArtifact(
            Resource resource,
            String fileName,
            MediaType contentType
    ) {
    }

    /**
     * プレビュー画面で利用する成果物内容と表示メタデータをまとめる値オブジェクトである。
     *
     * @param fileKey 参照対象成果物を識別するキー
     * @param fileName 表示対象の成果物ファイル名
     * @param contentType 成果物の Content-Type
     * @param content 成果物の文字列表現
     */
    public record PreviewArtifact(
            String fileKey,
            String fileName,
            MediaType contentType,
            String content,
            String scoreBandClass
    ) {
    }
}
