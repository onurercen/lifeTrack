package com.lifetrack.media.dto;

import com.lifetrack.media.entity.MediaStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MediaResponse(
    Long id,
    String title,
    String type,
    String url,
    String description,
    MediaStatus status,
    Integer rating,
    LocalDate finishedOn,
    String userEmail,
    LocalDateTime createdAt
) {
}
