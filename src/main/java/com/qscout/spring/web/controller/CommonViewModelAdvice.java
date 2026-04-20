package com.qscout.spring.web.controller;

import com.qscout.spring.web.dto.ExecutionLimitView;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class CommonViewModelAdvice {
    private static final ExecutionLimitView DEFAULT_LIMITS = new ExecutionLimitView(20, 60, "zip");

    @ModelAttribute("limits")
    public ExecutionLimitView limits() {
        return DEFAULT_LIMITS;
    }
}
