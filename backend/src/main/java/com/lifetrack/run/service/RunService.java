package com.lifetrack.run.service;

import com.lifetrack.common.exception.ApiException;
import com.lifetrack.run.dto.CreateRunRequest;
import com.lifetrack.run.dto.RunResponse;
import com.lifetrack.run.entity.Run;
import com.lifetrack.run.repository.RunRepository;
import com.lifetrack.user.entity.User;
import com.lifetrack.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RunService {

    private final RunRepository runRepository;
    private final UserRepository userRepository;

    public RunService(RunRepository runRepository, UserRepository userRepository) {
        this.runRepository = runRepository;
        this.userRepository = userRepository;
    }

    public RunResponse createRun(CreateRunRequest request, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException("Kullanıcı bulunamadı"));

        Run run = new Run();
        run.setDistanceKm(request.getDistanceKm());
        run.setDurationMinutes(request.getDurationMinutes());
        run.setCaloriesBurned(request.getCaloriesBurned());
        run.setNotes(request.getNotes() == null ? null : request.getNotes().trim());
        run.setUser(user);

        Run saved = runRepository.save(run);
        return toResponse(saved);
    }

    public List<RunResponse> getRunsByUser(String email) {
        return runRepository.findByUserEmailOrderByCreatedAtDesc(email)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private RunResponse toResponse(Run run) {
        return new RunResponse(
            run.getId(),
            run.getDistanceKm(),
            run.getDurationMinutes(),
            run.getCaloriesBurned(),
            run.getNotes(),
            run.getUser().getEmail(),
            run.getCreatedAt()
        );
    }
}
