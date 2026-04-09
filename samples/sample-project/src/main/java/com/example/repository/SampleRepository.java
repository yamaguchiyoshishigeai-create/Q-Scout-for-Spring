package com.example.repository;

import com.example.service.SampleService;

public interface SampleRepository {
    Object findAll();

    SampleService illegalDependency();
}
