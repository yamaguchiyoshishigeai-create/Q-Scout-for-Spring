package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerToRepositoryDirectAccessRuleTest {
    private final ControllerToRepositoryDirectAccessRule rule = new ControllerToRepositoryDirectAccessRule();

    @TempDir
    Path tempDir;

    @Test
    void flagsControllerThatCallsRepositoryDirectly() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object load() {
                        return sampleRepository.findAll();
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void ignoresControllerThatUsesOnlyService() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleService sampleService;

                    public Object load() {
                        return sampleService.findAll();
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    @Test
    void ignoresNonControllerClass() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/SampleService.java", """
                package com.example.service;

                public class SampleService {
                    private SampleRepository sampleRepository;
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    @Test
    void ignoresPlainStringMention() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    public String text() {
                        return "sampleRepository.findAll()";
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
