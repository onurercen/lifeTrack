package com.lifetrack.auth.repository;

import com.lifetrack.auth.entity.RefreshToken;
import com.lifetrack.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashWithUser(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user = :user and t.revokedAt is null")
    int revokeAllActive(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken t where t.user = :user and t.expiresAt < :now")
    int deleteExpired(@Param("user") User user, @Param("now") LocalDateTime now);

    long countByUserAndRevokedAtIsNull(User user);
}
