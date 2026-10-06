package com.lifetrack.book.dto;

import com.lifetrack.book.entity.BookStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookResponse(
    Long id,
    String title,
    String author,
    String description,
    BookStatus status,
    Integer pageCount,
    Integer currentPage,
    Integer rating,
    LocalDate startedOn,
    LocalDate finishedOn,
    String userEmail,
    LocalDateTime createdAt
) {
}
