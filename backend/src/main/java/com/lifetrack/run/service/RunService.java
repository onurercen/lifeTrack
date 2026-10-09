package com.lifetrack.run.service;

import com.lifetrack.common.exception.ApiException;
import com.lifetrack.common.web.PageResponse;
import com.lifetrack.run.dto.CreateRunRequest;
import com.lifetrack.run.dto.RunResponse;
import com.lifetrack.run.dto.RunSummaryResponse;
import com.lifetrack.run.entity.Run;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.lifetrack.common.util.Strings.trimToNull;

@Service
public class RunService {

    // Tolerates a phone clock that runs slightly ahead of the server's.
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    private final RunRepository runRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public RunService(RunRepository runRepository, UserRepository userRepository, Clock clock) {
        this.runRepository = runRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public RunResponse createRun(CreateRunRequest request, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> ApiException.notFound("Kullanıcı bulunamadı"));

        Run run = new Run();
        applyRequest(run, request);
        if (run.getRunAt() == null) {
            run.setRunAt(LocalDateTime.now(clock));
        }
        run.setUser(user);

        Run saved = runRepository.save(run);
        return toResponse(saved);
    }

    public RunResponse updateRun(Long id, CreateRunRequest request, String email) {
        Run run = findOwnedRun(id, email);
        applyRequest(run, request);
        return toResponse(runRepository.save(run));
    }

    public void deleteRun(Long id, String email) {
        runRepository.delete(findOwnedRun(id, email));
    }

    public PageResponse<RunResponse> getRuns(String email, Pageable pageable) {
        return PageResponse.of(runRepository.findByUserEmailOrderByRunAtDescIdDesc(email, pageable).map(this::toResponse));
    }

    /** Every run, newest first; for the data export. */
    public List<RunResponse> getAllRuns(String email) {
        return runRepository.findByUserEmailOrderByRunAtDescIdDesc(email)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /** Totals over all runs, since a page of the list only holds some of them. */
    public RunSummaryResponse getSummary(String email) {
        return new RunSummaryResponse(
            runRepository.countByUserEmail(email),
            runRepository.sumDistanceByUserEmail(email),
            runRepository.sumDurationByUserEmail(email)
        );
    }

    // Another user's run is reported as missing, so ids can't be probed.
    private Run findOwnedRun(Long id, String email) {
        return runRepository.findByIdAndUserEmail(id, email)
            .orElseThrow(() -> ApiException.notFound("Koşu bulunamadı"));
    }

    private void applyRequest(Run run, CreateRunRequest request) {
        run.setDistanceKm(request.getDistanceKm());
        run.setDurationMinutes(request.getDurationMinutes());
        run.setCaloriesBurned(request.getCaloriesBurned());
        run.setNotes(trimToNull(request.getNotes()));
        // On update, an omitted runAt keeps the existing value.
        if (request.getRunAt() != null) {
            if (request.getRunAt().isAfter(LocalDateTime.now(clock).plus(FUTURE_TOLERANCE))) {
                throw ApiException.badRequest("Koşu tarihi gelecekte olamaz");
            }
            run.setRunAt(request.getRunAt());
        }
    }

    private RunResponse toResponse(Run run) {
        return new RunResponse(
            run.getId(),
            run.getDistanceKm(),
            run.getDurationMinutes(),
            run.getCaloriesBurned(),
            run.getNotes(),
            run.getUser().getEmail(),
            run.getRunAt(),
            run.getCreatedAt()
        );
    }
}
