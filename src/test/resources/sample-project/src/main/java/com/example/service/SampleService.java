package com.example.service;

import org.springframework.stereotype.Service;

@Service
public class SampleService {
    public void execute() {
        try {
            risky();
        } catch (Exception ex) {
            // ignored
        }
    }

    private void risky() {
    }
}
