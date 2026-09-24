package com.lifetrack.run.repository;

import com.lifetrack.run.entity.Run;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunRepository extends JpaRepository<Run, Long> {

    List<Run> findByUserEmailOrderByCreatedAtDesc(String email);
}
