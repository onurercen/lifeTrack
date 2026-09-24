package com.lifetrack.media.controller;

import com.lifetrack.media.dto.CreateMediaRequest;
import com.lifetrack.media.dto.MediaResponse;
import com.lifetrack.media.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/media/health")
    public String health() {
        return "Media service is running";
    }

    @GetMapping("/media/search")
    public ResponseEntity<List<MediaResponse>> searchMedia(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(mediaService.searchMedia(query, getCurrentUserEmail()));
    }

    @GetMapping("/users/media")
    public ResponseEntity<List<MediaResponse>> getUserMedia() {
        return ResponseEntity.ok(mediaService.getUserMedia(getCurrentUserEmail()));
    }

    @PostMapping("/user/media")
    public ResponseEntity<MediaResponse> createMedia(@Valid @RequestBody CreateMediaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mediaService.createMedia(request, getCurrentUserEmail()));
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
