package com.qscout.spring.infrastructure;

import com.qscout.spring.application.ProjectScanner;
import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.util.FileCollector;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DefaultProjectScanner implements ProjectScanner {
    @Override
    public ProjectContext scan(AnalysisRequest request) {
        Path projectRoot = request.projectRootPath().toAbsolutePath().normalize();
        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("Project root does not exist: " + projectRoot);
        }

        Path pomXml = projectRoot.resolve("pom.xml");
        if (!Files.exists(pomXml)) {
            throw new IllegalArgumentException("pom.xml was not found under: " + projectRoot);
        }

        return new ProjectContext(
                projectRoot,
                pomXml,
                FileCollector.collectJavaFiles(projectRoot.resolve("src/main/java")),
                FileCollector.collectJavaFiles(projectRoot.resolve("src/test/java"))
        );
    }
}
