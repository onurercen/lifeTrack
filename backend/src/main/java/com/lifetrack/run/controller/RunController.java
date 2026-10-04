package com.lifetrack.run.controller;

import com.lifetrack.run.dto.CreateRunRequest;
import com.lifetrack.run.dto.RunResponse;
import com.lifetrack.run.service.RunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping
    public ResponseEntity<List<RunResponse>> getRuns(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(runService.getRunsByUser(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<RunResponse> createRun(
        @Valid @RequestBody CreateRunRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(runService.createRun(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RunResponse> updateRun(
        @PathVariable Long id,
        @Valid @RequestBody CreateRunRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(runService.updateRun(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRun(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        runService.deleteRun(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
