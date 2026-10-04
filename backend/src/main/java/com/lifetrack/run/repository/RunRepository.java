package com.lifetrack.run.repository;

import com.lifetrack.run.entity.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RunRepository extends JpaRepository<Run, Long> {

    List<Run> findByUserEmailOrderByRunAtDescIdDesc(String email);

    Optional<Run> findByIdAndUserEmail(Long id, String email);

    List<Run> findByUserEmailAndRunAtGreaterThanEqual(String email, LocalDateTime from);

    long countByUserEmail(String email);

    @Query("select coalesce(sum(r.distanceKm), 0) from Run r where r.user.email = :email")
    double sumDistanceByUserEmail(@Param("email") String email);
}
