package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.ArtifactExpiredException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            "human", new ArtifactDefinition("qscout-report.md", MediaType.TEXT_MARKDOWN),
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
        ResolvedArtifact artifact = resolveArtifact(requestId, fileKey);
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
        ResolvedArtifact artifact = resolveArtifact(requestId, fileKey);
        try {
            String content = Files.readString(artifact.path(), StandardCharsets.UTF_8);
            return new PreviewArtifact(artifact.fileKey(), artifact.fileName(), artifact.contentType(), content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read preview artifact.", exception);
        }
    }

    private ResolvedArtifact resolveArtifact(String requestId, String fileKey) {
        ArtifactDefinition definition = FILE_MAPPING.get(fileKey);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid download file key.");
        }

        tempWorkspaceService.assertActive(requestId);
        Path rootDir = tempWorkspaceService.resolveWorkspaceRoot(requestId);
        Path artifactPath = rootDir.resolve("output").resolve(definition.fileName()).normalize();
        if (!artifactPath.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid download path.");
        }
        if (!Files.exists(artifactPath) || !Files.isRegularFile(artifactPath)) {
            throw new ArtifactExpiredException("ダウンロード期限が切れました。再度解析を実行してください。");
        }
        return new ResolvedArtifact(fileKey, artifactPath, definition.fileName(), definition.contentType());
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
            String content
    ) {
    }
}
