package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.ProjectContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultProjectScannerTest {
    private final DefaultProjectScanner scanner = new DefaultProjectScanner();

    @TempDir
    Path tempDir;

    @Test
    void scansMavenProjectAndCollectsMainAndTestJavaFiles() throws IOException {
        Path projectRoot = tempDir.resolve("project");
        Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.createDirectories(projectRoot.resolve("src/test/java/com/example"));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>");
        Files.writeString(projectRoot.resolve("src/main/java/com/example/App.java"), "class App {}\n");
        Files.writeString(projectRoot.resolve("src/test/java/com/example/AppTest.java"), "class AppTest {}\n");

        ProjectContext context = scanner.scan(new AnalysisRequest(projectRoot, projectRoot.resolve("out")));

        assertThat(context.projectRootPath()).isEqualTo(projectRoot.toAbsolutePath().normalize());
        assertThat(context.pomXmlPath()).isEqualTo(projectRoot.resolve("pom.xml").toAbsolutePath().normalize());
        assertThat(context.mainJavaFiles()).hasSize(1);
        assertThat(context.testJavaFiles()).hasSize(1);
    }

    @Test
    void failsWhenPomXmlIsMissing() {
        Path projectRoot = tempDir.resolve("missing-pom");
        try {
            Files.createDirectories(projectRoot);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

        assertThatThrownBy(() -> scanner.scan(new AnalysisRequest(projectRoot, projectRoot.resolve("out"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pom.xml");
    }

    @Test
    void returnsEmptyListsWhenSourceDirectoriesDoNotExist() throws IOException {
        Path projectRoot = tempDir.resolve("empty-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>");

        ProjectContext context = scanner.scan(new AnalysisRequest(projectRoot, projectRoot.resolve("out")));

        assertThat(context.mainJavaFiles()).isEmpty();
        assertThat(context.testJavaFiles()).isEmpty();
    }
}
