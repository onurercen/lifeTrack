package com.lifetrack.media.repository;

import com.lifetrack.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByUserEmailOrderByCreatedAtDesc(String email);

    Optional<Media> findByIdAndUserEmail(Long id, String email);

    long countByUserEmail(String email);

    @Query("""
        select m from Media m
        where m.user.email = :email
          and (lower(m.title) like lower(concat('%', :query, '%'))
            or lower(m.type) like lower(concat('%', :query, '%'))
            or lower(m.description) like lower(concat('%', :query, '%')))
        order by m.createdAt desc
        """)
    List<Media> search(@Param("email") String email, @Param("query") String query);
}
