package com.example.controller;

import com.example.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@Transactional
public class SampleController {
    @Autowired
    private SampleRepository sampleRepository;

    public Object findAll() {
        return sampleRepository.findAll();
    }
}
