package com.qscout.spring.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CodeSnippetExtractor {
    private CodeSnippetExtractor() {
    }

    public static String extract(Path filePath, int centerLine) {
        return extract(filePath, centerLine, 2);
    }

    public static String extract(Path filePath, int centerLine, int radius) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            if (lines.isEmpty()) {
                return "";
            }
            int safeLine = Math.max(1, centerLine);
            int start = Math.max(1, safeLine - radius);
            int end = Math.min(lines.size(), safeLine + radius);
            StringBuilder builder = new StringBuilder();
            for (int line = start; line <= end; line++) {
                builder.append(line)
                        .append(": ")
                        .append(lines.get(line - 1))
                        .append(System.lineSeparator());
            }
            return builder.toString().trim();
        } catch (IOException exception) {
            return "";
        }
    }
}
