package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackageDependencyViolationRuleTest {
    private final PackageDependencyViolationRule rule = new PackageDependencyViolationRule();

    @TempDir
    Path tempDir;

    @Test
    void ignoresForwardLayerDependency() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import com.example.service.SampleService;

                public class SampleController {
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    @Test
    void flagsControllerImportingRepository() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import com.example.repository.SampleRepository;

                public class SampleController {
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void flagsRepositoryDependingOnUpperLayer() throws IOException {
        Path file = writeSource("src/main/java/com/example/repository/SampleRepository.java", """
                package com.example.repository;

                import com.example.controller.SampleController;

                public class SampleRepository {
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void ignoresUnrelatedClass() throws IOException {
        Path file = writeSource("src/main/java/com/example/util/Helper.java", """
                package com.example.util;

                import com.example.controller.SampleController;

                public class Helper {
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
