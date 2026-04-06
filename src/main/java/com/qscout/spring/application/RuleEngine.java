package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;

public interface RuleEngine {
    AnalysisResult analyze(ProjectContext projectContext);
}
