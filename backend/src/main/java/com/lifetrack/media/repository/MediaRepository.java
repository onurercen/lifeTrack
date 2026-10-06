package com.lifetrack.media.repository;

import com.lifetrack.media.entity.Media;
import com.lifetrack.media.entity.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByIdAndUserEmail(Long id, String email);

    long countByUserEmail(String email);

    long countByUserEmailAndStatus(String email, MediaStatus status);

    long countByUserEmailAndStatusAndFinishedOnGreaterThanEqual(String email, MediaStatus status, LocalDate from);

    /** An empty [query] matches every entry; a null [status] matches every status. */
    @Query("""
        select m from Media m
        where m.user.email = :email
          and (:status is null or m.status = :status)
          and (lower(m.title) like lower(concat('%', :query, '%'))
            or lower(m.type) like lower(concat('%', :query, '%'))
            or lower(m.description) like lower(concat('%', :query, '%')))
        order by m.createdAt desc, m.id desc
        """)
    List<Media> search(@Param("email") String email, @Param("query") String query, @Param("status") MediaStatus status);
}
