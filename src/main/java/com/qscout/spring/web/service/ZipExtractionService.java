package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidProjectStructureException;
import com.qscout.spring.web.exception.InvalidUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipExtractionService {
    public void saveUpload(MultipartFile file, Path uploadZipPath) {
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, uploadZipPath);
        } catch (IOException exception) {
            throw new InvalidUploadException("error.invalidUpload.saveFailed", "アップロードファイルの保存に失敗しました。", exception);
        }
    }

    public void extract(Path zipPath, Path extractedDir) {
        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = extractedDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(extractedDir.normalize())) {
                    throw new InvalidUploadException("error.invalidUpload.illegalPath", "zip内に不正なパスが含まれています。");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, target);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException exception) {
            throw new InvalidUploadException(
                    "error.invalidUpload.unreadableArchive",
                    "zipファイルの解凍に失敗しました。破損していないか確認してください。",
                    exception
            );
        }
    }

    public Path resolveProjectRoot(Path extractedDir) {
        Path directPom = extractedDir.resolve("pom.xml");
        if (Files.exists(directPom)) {
            return extractedDir;
        }

        List<Path> candidates = new ArrayList<>();
        try (var stream = Files.list(extractedDir)) {
            stream.filter(Files::isDirectory)
                    .map(path -> path.resolve("pom.xml"))
                    .filter(Files::exists)
                    .map(Path::getParent)
                    .forEach(candidates::add);
        } catch (IOException exception) {
            throw new InvalidProjectStructureException("error.projectStructure.resolveFailed", "プロジェクトルートの判定に失敗しました。");
        }

        if (candidates.isEmpty()) {
            throw new InvalidProjectStructureException(
                    "error.projectStructure.pomNotFound",
                    "pom.xml が見つかりません。Spring Boot / Mavenプロジェクトをアップロードしてください。"
            );
        }
        if (candidates.size() > 1) {
            throw new InvalidProjectStructureException(
                    "error.projectStructure.multipleCandidates",
                    "pom.xml を持つ候補が複数見つかりました。単一プロジェクトのzipを指定してください。"
            );
        }
        return candidates.get(0).normalize();
    }
}
