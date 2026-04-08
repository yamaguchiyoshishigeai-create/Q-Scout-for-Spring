package com.qscout.spring.rule;

import com.qscout.spring.domain.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionSwallowingRuleTest {
    private final ExceptionSwallowingRule rule = new ExceptionSwallowingRule();

    @TempDir
    Path tempDir;

    @Test
    void flagsEmptyCatchBlock() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/EmptyCatchService.java", """
                package com.example.service;

                public class EmptyCatchService {
                    void execute() {
                        try {
                            work();
                        } catch (Exception ex) {
                        }
                    }

                    void work() {
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
    }

    @Test
    void doesNotFlagRethrow() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/RethrowService.java", """
                package com.example.service;

                public class RethrowService {
                    void execute() {
                        try {
                            work();
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    }

                    void work() {
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    @Test
    void doesNotFlagCleanupWithLogging() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/CleanupService.java", """
                package com.example.service;

                public class CleanupService {
                    private static final DummyLogger log = new DummyLogger();

                    void execute() {
                        try {
                            work();
                        } catch (Exception ex) {
                            cleanup();
                            log.warn("ignored", ex);
                        }
                    }

                    void work() {
                    }

                    void cleanup() {
                    }

                    static class DummyLogger {
                        void warn(String message, Exception exception) {
                        }
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).isEmpty();
    }

    @Test
    void flagsReturnNullFallback() throws IOException {
        Path file = writeSource("src/main/java/com/example/service/ReturnNullService.java", """
                package com.example.service;

                public class ReturnNullService {
                    String execute() {
                        try {
                            work();
                            return "ok";
                        } catch (Exception ex) {
                            return null;
                        }
                    }

                    void work() {
                    }
                }
                """);

        assertThat(rule.evaluate(context(file)).violations()).hasSize(1);
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
