package com.lifetrack.dashboard.service;

import com.lifetrack.book.repository.BookRepository;
import com.lifetrack.dashboard.dto.DashboardResponse;
import com.lifetrack.dashboard.dto.DashboardResponse.DailyDistance;
import com.lifetrack.dashboard.dto.DashboardResponse.RunStats;
import com.lifetrack.media.repository.MediaRepository;
import com.lifetrack.run.entity.Run;
import com.lifetrack.run.repository.RunRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int WINDOW_DAYS = 7;

    private final RunRepository runRepository;
    private final BookRepository bookRepository;
    private final MediaRepository mediaRepository;
    private final Clock clock;

    public DashboardService(
        RunRepository runRepository,
        BookRepository bookRepository,
        MediaRepository mediaRepository,
        Clock clock
    ) {
        this.runRepository = runRepository;
        this.bookRepository = bookRepository;
        this.mediaRepository = mediaRepository;
        this.clock = clock;
    }

    public DashboardResponse getDashboard(String email) {
        return new DashboardResponse(
            runStats(email),
            bookRepository.countByUserEmail(email),
            mediaRepository.countByUserEmail(email)
        );
    }

    // "Week" is a rolling window: today and the six days before it.
    private RunStats runStats(String email) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(WINDOW_DAYS - 1);
        List<Run> recent = runRepository.findByUserEmailAndRunAtGreaterThanEqual(email, firstDay.atStartOfDay());

        Map<LocalDate, Double> distanceByDay = recent.stream()
            .collect(Collectors.groupingBy(run -> run.getRunAt().toLocalDate(),
                Collectors.summingDouble(Run::getDistanceKm)));

        List<DailyDistance> lastSevenDays = firstDay.datesUntil(today.plusDays(1))
            .map(day -> new DailyDistance(day, distanceByDay.getOrDefault(day, 0.0)))
            .toList();

        return new RunStats(
            runRepository.countByUserEmail(email),
            runRepository.sumDistanceByUserEmail(email),
            recent.size(),
            recent.stream().mapToDouble(Run::getDistanceKm).sum(),
            recent.stream().mapToInt(Run::getDurationMinutes).sum(),
            lastSevenDays
        );
    }
}
