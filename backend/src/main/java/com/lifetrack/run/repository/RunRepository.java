package com.lifetrack.run.repository;

import com.lifetrack.run.entity.Run;
import com.lifetrack.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RunRepository extends JpaRepository<Run, Long> {

    List<Run> findByUserEmailOrderByRunAtDescIdDesc(String email);

    Page<Run> findByUserEmailOrderByRunAtDescIdDesc(String email, Pageable pageable);

    Optional<Run> findByIdAndUserEmail(Long id, String email);

    List<Run> findByUserEmailAndRunAtGreaterThanEqual(String email, LocalDateTime from);

    long countByUserEmail(String email);

    @Query("select coalesce(sum(r.distanceKm), 0) from Run r where r.user.email = :email")
    double sumDistanceByUserEmail(@Param("email") String email);

    @Query("select coalesce(sum(r.durationMinutes), 0) from Run r where r.user.email = :email")
    long sumDurationByUserEmail(@Param("email") String email);

    @Modifying
    @Query("delete from Run r where r.user = :user")
    int deleteAllByOwner(@Param("user") User user);
}
