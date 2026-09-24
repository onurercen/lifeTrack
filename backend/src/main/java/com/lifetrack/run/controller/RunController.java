package com.lifetrack.run.controller;

import com.lifetrack.run.dto.CreateRunRequest;
import com.lifetrack.run.dto.RunResponse;
import com.lifetrack.run.service.RunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping("/runs/health")
    public String health() {
        return "Run service is running";
    }

    @GetMapping("/runs")
    public ResponseEntity<List<RunResponse>> getRuns() {
        return ResponseEntity.ok(runService.getRunsByUser(getCurrentUserEmail()));
    }

    @PostMapping("/runs")
    public ResponseEntity<RunResponse> createRun(@Valid @RequestBody CreateRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(runService.createRun(request, getCurrentUserEmail()));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
