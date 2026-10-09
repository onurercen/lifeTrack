package com.lifetrack.user.dto;

import com.lifetrack.book.dto.BookResponse;
import com.lifetrack.media.dto.MediaResponse;
import com.lifetrack.run.dto.RunResponse;

import java.time.LocalDateTime;
import java.util.List;

/** A copy of all of a user's data, downloaded from /api/users/me/export. */
public record DataExport(
    LocalDateTime exportedAt,
    Account account,
    List<RunResponse> runs,
    List<BookResponse> books,
    List<MediaResponse> media
) {

    public record Account(String name, String email, LocalDateTime createdAt) {
    }
}
