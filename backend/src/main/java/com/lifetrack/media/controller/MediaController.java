package com.lifetrack.media.controller;

import com.lifetrack.media.dto.CreateMediaRequest;
import com.lifetrack.media.dto.MediaResponse;
import com.lifetrack.media.service.MediaService;
import com.lifetrack.media.entity.MediaStatus;
import com.lifetrack.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<MediaResponse>> getMedia(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) MediaStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "" + PageResponse.DEFAULT_SIZE) int size,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(mediaService.searchMedia(query, status, principal.getUsername(), PageResponse.request(page, size)));
    }

    @PostMapping
    public ResponseEntity<MediaResponse> createMedia(
        @Valid @RequestBody CreateMediaRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mediaService.createMedia(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MediaResponse> updateMedia(
        @PathVariable Long id,
        @Valid @RequestBody CreateMediaRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(mediaService.updateMedia(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        mediaService.deleteMedia(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
