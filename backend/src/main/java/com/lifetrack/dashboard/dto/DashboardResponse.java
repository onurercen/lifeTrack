package com.lifetrack.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
    RunStats runs,
    BookStats books,
    MediaStats media
) {

    public record RunStats(
        long totalCount,
        double totalDistanceKm,
        int weekCount,
        double weekDistanceKm,
        int weekDurationMinutes,
        List<DailyDistance> lastSevenDays
    ) {
    }

    public record DailyDistance(LocalDate date, double distanceKm) {
    }

    public record BookStats(
        long totalCount,
        long readingCount,
        long finishedThisYear,
        List<ReadingBook> currentlyReading
    ) {
    }

    public record ReadingBook(Long id, String title, String author, Integer currentPage, Integer pageCount) {
    }

    public record MediaStats(long totalCount, long inProgressCount, long completedThisYear) {
    }
}
