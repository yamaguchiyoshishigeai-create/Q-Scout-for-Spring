package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldInjectionRuleTest {
    private final FieldInjectionRule rule = new FieldInjectionRule();

    @TempDir
    Path tempDir;

    @Test
    void flagsAutowiredFieldOutsideRulePackage() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.beans.factory.annotation.Autowired;

                public class SampleController {
                    @Autowired
                    private Object dependency;
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void ignoresRulePackageSelfAnalysisNoise() throws IOException {
        Path file = writeSource("src/main/java/com/qscout/spring/rule/FieldInjectionRule.java", """
                package com.qscout.spring.rule;

                public class FieldInjectionRule {
                    void evaluate(String line) {
                        if (line.contains("@Autowired")) {
                        }
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    private ProjectContext context(Path mainFile) {
        return new ProjectContext(tempDir, tempDir.resolve("pom.xml"), List.of(mainFile), List.of());
    }

    private Path writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
