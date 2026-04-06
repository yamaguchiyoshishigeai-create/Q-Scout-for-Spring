package com.qscout.spring.domain;

import java.nio.file.Path;
import java.util.List;

public record ProjectContext(
        Path projectRootPath,
        Path pomXmlPath,
        List<Path> mainJavaFiles,
        List<Path> testJavaFiles
) {
}
