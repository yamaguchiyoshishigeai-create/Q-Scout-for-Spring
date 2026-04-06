package com.qscout.spring.cli;

import com.qscout.spring.domain.AnalysisRequest;

import java.nio.file.Path;

public class ArgumentParser {
    public AnalysisRequest parse(String[] args) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException(usage());
        }
        return new AnalysisRequest(Path.of(args[0]), Path.of(args[1]));
    }

    public String usage() {
        return "Usage: java -jar q-scout-for-spring.jar <project-root> <output-directory>";
    }
}
