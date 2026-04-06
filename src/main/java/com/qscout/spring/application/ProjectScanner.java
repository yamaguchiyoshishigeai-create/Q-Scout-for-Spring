package com.qscout.spring.application;

import com.qscout.spring.domain.AnalysisRequest;
import com.qscout.spring.domain.ProjectContext;

public interface ProjectScanner {
    ProjectContext scan(AnalysisRequest request);
}
