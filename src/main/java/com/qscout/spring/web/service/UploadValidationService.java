package com.qscout.spring.web.service;

import com.qscout.spring.web.exception.InvalidUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipInputStream;

@Service
public class UploadValidationService {
    public static final long MAX_UPLOAD_SIZE_BYTES = 20L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("zipファイルを選択してください。");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new InvalidUploadException("アップロード可能なのはzipファイルのみです。");
        }

        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new InvalidUploadException("ファイルサイズが上限を超えています。20MB以下のzipを指定してください。");
        }

        try (InputStream inputStream = file.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            if (zipInputStream.getNextEntry() == null) {
                throw new InvalidUploadException("zipファイルの中身が空です。解析対象プロジェクトを含めてください。");
            }
        } catch (IOException exception) {
            throw new InvalidUploadException("zipファイルの解凍に失敗しました。破損していないか確認してください。", exception);
        }
    }
}
