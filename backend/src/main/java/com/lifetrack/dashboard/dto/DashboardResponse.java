package com.lifetrack.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
    RunStats runs,
    long bookCount,
    long mediaCount
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
}
