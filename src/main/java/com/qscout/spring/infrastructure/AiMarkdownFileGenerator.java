package com.qscout.spring.infrastructure;

import com.qscout.spring.application.AiMarkdownGenerator;
import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.util.MarkdownWriter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class AiMarkdownFileGenerator implements AiMarkdownGenerator {
    @Override
    public Path generate(AnalysisResult analysisResult, Path outputDirectory) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Project Analysis Input").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("## Project Summary").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Project Root: ").append(analysisResult.projectContext().projectRootPath()).append(System.lineSeparator());
        builder.append("- Main Java Files: ").append(analysisResult.projectContext().mainJavaFiles().size()).append(System.lineSeparator());
        builder.append("- Test Java Files: ").append(analysisResult.projectContext().testJavaFiles().size()).append(System.lineSeparator());
        builder.append("- Total Issues: ").append(analysisResult.allViolations().size()).append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("## Detected Issues").append(System.lineSeparator()).append(System.lineSeparator());
        if (analysisResult.allViolations().isEmpty()) {
            builder.append("No issues detected.").append(System.lineSeparator()).append(System.lineSeparator());
        }

        int index = 1;
        for (Violation violation : analysisResult.allViolations()) {
            builder.append("### Issue ").append(index++).append(System.lineSeparator()).append(System.lineSeparator());
            builder.append("- Rule: ").append(violation.ruleName()).append(System.lineSeparator());
            builder.append("- Severity: ").append(violation.severity()).append(System.lineSeparator());
            builder.append("- File: ").append(violation.filePath()).append(System.lineSeparator());
            builder.append("- Line: ").append(violation.lineNumber()).append(System.lineSeparator());
            builder.append("- Message: ").append(violation.message()).append(System.lineSeparator());
            if (!violation.codeSnippet().isBlank()) {
                builder.append(System.lineSeparator()).append("```java").append(System.lineSeparator());
                builder.append(violation.codeSnippet()).append(System.lineSeparator());
                builder.append("```").append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
        }

        builder.append("## Instructions").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- Explain why each issue is problematic.").append(System.lineSeparator());
        builder.append("- Suggest improvements.").append(System.lineSeparator());
        return MarkdownWriter.write(outputDirectory, "qscout-ai-input.md", builder.toString());
    }
}
