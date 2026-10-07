package com.lifetrack.auth.service;

import com.lifetrack.auth.entity.RefreshToken;
import com.lifetrack.auth.repository.RefreshTokenRepository;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque, single-use refresh tokens. Each use returns the user and revokes the
 * token; the caller issues a new one (rotation). Presenting an already used token
 * revokes all of the user's sessions, since a copy of it may have been stolen.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration validity;

    public RefreshTokenService(
        RefreshTokenRepository repository,
        Clock clock,
        @Value("${jwt.refresh-expiration-days:30}") long validityDays
    ) {
        this.repository = repository;
        this.clock = clock;
        this.validity = Duration.ofDays(validityDays);
    }

    /** Creates a token for [user] and returns its raw value; only the hash is stored. */
    @Transactional
    public String issue(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.deleteExpired(user, now);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(user, hash(raw), now, now.plus(validity)));
        return raw;
    }

    /** Uses up [raw] and returns its owner. Not rolled back on failure so reuse revocation sticks. */
    @Transactional(noRollbackFor = ApiException.class)
    public User consume(String raw) {
        RefreshToken token = repository.findByTokenHashWithUser(hash(raw)).orElseThrow(RefreshTokenService::invalid);
        LocalDateTime now = LocalDateTime.now(clock);

        if (token.getRevokedAt() != null) {
            repository.revokeAllActive(token.getUser(), now);
            throw invalid();
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw invalid();
        }
        token.setRevokedAt(now);
        return token.getUser();
    }

    /** Ends the session of [raw]; unknown or already revoked tokens are ignored. */
    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHashWithUser(hash(raw))
            .filter(token -> token.getRevokedAt() == null)
            .ifPresent(token -> token.setRevokedAt(LocalDateTime.now(clock)));
    }

    /** Signs [user] out everywhere. */
    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllActive(user, LocalDateTime.now(clock));
    }

    private static ApiException invalid() {
        return ApiException.unauthorized("Oturumunuzun süresi doldu, lütfen tekrar giriş yapın");
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
