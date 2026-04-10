package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
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
    void classifiesFindAllAsLowSeverity() throws IOException {
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

        assertViolation(file, Severity.LOW, "単純な read-only");
    }

    @Test
    void classifiesFindByIdAsMediumSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object load(Long id) {
                        return sampleRepository.findById(id).orElseThrow();
                    }
                }
                """);

        assertViolation(file, Severity.MEDIUM, "Repository を直接参照");
    }

    @Test
    void classifiesSaveAsHighSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object save(Object entity) {
                        return sampleRepository.save(entity);
                    }
                }
                """);

        assertViolation(file, Severity.HIGH, "書き込みやユースケース級");
    }

    @Test
    void classifiesMutationAndSaveAsHighSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object update(Long id) {
                        SampleEntity entity = sampleRepository.findById(id).orElseThrow();
                        entity.setName("updated");
                        return sampleRepository.save(entity);
                    }
                }
                """);

        List<Violation> violations = rule.evaluate(context(file)).violations();
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(Violation::severity).contains(Severity.HIGH);
        assertThat(violations).filteredOn(v -> v.lineNumber() != null && v.lineNumber() > 0).hasSize(2);
    }

    @Test
    void classifiesModelAttributeReadAsLowSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.ModelAttribute;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    @ModelAttribute("items")
                    public Object populateItems() {
                        return sampleRepository.findAll();
                    }
                }
                """);

        assertViolation(file, Severity.LOW, "単純な read-only");
    }

    @Test
    void ignoresControllerWithoutRepositoryDependency() throws IOException {
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
    void canReportMediumAndHighInSameController() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object load(Long id) {
                        return sampleRepository.findById(id).orElseThrow();
                    }

                    public Object save(Object entity) {
                        return sampleRepository.save(entity);
                    }
                }
                """);

        List<Violation> violations = rule.evaluate(context(file)).violations();
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(Violation::severity)
                .containsExactlyInAnyOrder(Severity.MEDIUM, Severity.HIGH);
    }

    @Test
    void classifiesFindByWithOrElseThrowAsMediumSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object load(String name) {
                        return sampleRepository.findByName(name).orElseThrow();
                    }
                }
                """);

        assertViolation(file, Severity.MEDIUM, "Repository を直接参照");
    }

    @Test
    void classifiesPopulateMethodReadAsLowSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public Object populateItems() {
                        return sampleRepository.findByCategory("master");
                    }
                }
                """);

        assertViolation(file, Severity.LOW, "単純な read-only");
    }

    @Test
    void classifiesDeleteByIdAsHighSeverity() throws IOException {
        Path file = writeSource("src/main/java/com/example/controller/SampleController.java", """
                package com.example.controller;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class SampleController {
                    private SampleRepository sampleRepository;

                    public void delete(Long id) {
                        sampleRepository.deleteById(id);
                    }
                }
                """);

        assertViolation(file, Severity.HIGH, "書き込みやユースケース級");
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

    private void assertViolation(Path file, Severity severity, String messageFragment) {
        List<Violation> violations = rule.evaluate(context(file)).violations();
        assertThat(violations).hasSize(1);
        Violation violation = violations.get(0);
        assertThat(violation.severity()).isEqualTo(severity);
        assertThat(violation.message()).contains(messageFragment);
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
