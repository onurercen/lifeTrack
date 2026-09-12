package com.lifetrack.run.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    @GetMapping("/health")
    public String health() {
        return "Run service is running";
    }
}
