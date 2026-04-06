package com.qscout.spring.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MarkdownWriter {
    private MarkdownWriter() {
    }

    public static Path write(Path outputDirectory, String fileName, String content) {
        try {
            Files.createDirectories(outputDirectory);
            Path filePath = outputDirectory.resolve(fileName);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return filePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write markdown file: " + fileName, exception);
        }
    }
}
