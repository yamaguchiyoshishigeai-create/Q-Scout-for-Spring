package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingTestRuleTest {
    private final MissingTestRule rule = new MissingTestRule();

    @TempDir
    Path tempDir;

    @Test
    void requiresTestsOnlyForTargetConcreteClasses() throws IOException {
        List<Path> mainFiles = new ArrayList<>();
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/web/service/WebAnalysisService.java", "public class WebAnalysisService {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/web/controller/WebPageController.java", "public class WebPageController {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/application/SharedAnalysisService.java", "public class SharedAnalysisService {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/infrastructure/DefaultProjectScanner.java", "public class DefaultProjectScanner {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/rule/TransactionalMisuseRule.java", "public class TransactionalMisuseRule {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/web/dto/WebAnalysisResponse.java", "public record WebAnalysisResponse(String value) {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/web/exception/InvalidUploadException.java", "public class InvalidUploadException extends RuntimeException {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/domain/Violation.java", "public class Violation {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/rule/AbstractTextRule.java", "public abstract class AbstractTextRule {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/rule/Rule.java", "public interface Rule {}"));
        mainFiles.add(writeSource("src/main/java/com/qscout/spring/cli/Main.java", "public class Main {}"));

        List<Path> testFiles = List.of(
                writeSource("src/test/java/com/qscout/spring/web/service/WebAnalysisServiceTest.java", "class WebAnalysisServiceTest {}"),
                writeSource("src/test/java/com/qscout/spring/rule/TransactionalMisuseRuleTest.java", "class TransactionalMisuseRuleTest {}")
        );

        RuleResult result = rule.evaluate(new ProjectContext(tempDir, tempDir.resolve("pom.xml"), mainFiles, testFiles));

        assertThat(result.violations())
                .extracting(violation -> violation.filePath().getFileName().toString())
                .containsExactlyInAnyOrder(
                        "WebPageController.java",
                        "SharedAnalysisService.java",
                        "DefaultProjectScanner.java"
                );
    }

    private Path writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
