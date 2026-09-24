package com.lifetrack.media.repository;

import com.lifetrack.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByUserEmailOrderByCreatedAtDesc(String email);
}
