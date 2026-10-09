package com.lifetrack.auth.service;

import com.lifetrack.auth.entity.EmailCode;
import com.lifetrack.auth.entity.EmailCodePurpose;
import com.lifetrack.auth.repository.EmailCodeRepository;
import com.lifetrack.common.exception.ApiException;
import com.lifetrack.user.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Six-digit codes sent by e-mail. A code is valid for {@link #VALIDITY} and is
 * dropped after {@link #MAX_ATTEMPTS} wrong guesses, so guessing one in time is
 * practically impossible. A new code can be requested every {@link #RESEND_COOLDOWN}.
 */
@Service
public class EmailCodeService {

    public static final Duration VALIDITY = Duration.ofMinutes(15);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailCodeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public EmailCodeService(EmailCodeRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates a code for [user], replacing any earlier one, and returns it.
     * Throws 429 if the previous code was issued less than a cooldown ago.
     */
    @Transactional
    public String issue(User user, EmailCodePurpose purpose) {
        LocalDateTime now = LocalDateTime.now(clock);
        EmailCode code = repository.findByUserAndPurpose(user, purpose).orElse(null);
        if (code != null) {
            Duration remaining = Duration.between(now, code.getCreatedAt().plus(RESEND_COOLDOWN));
            if (!remaining.isNegative() && !remaining.isZero()) {
                throw ApiException.tooManyRequests(
                    "Yeni kod için " + Math.max(1, remaining.toSeconds()) + " saniye bekleyin.",
                    remaining
                );
            }
        } else {
            code = new EmailCode(user, purpose);
        }

        String raw = String.format("%06d", RANDOM.nextInt(1_000_000));
        code.setCodeHash(passwordEncoder.encode(raw));
        code.setAttempts(0);
        code.setCreatedAt(now);
        code.setExpiresAt(now.plus(VALIDITY));
        repository.save(code);
        return raw;
    }

    /**
     * Uses up the code if [raw] matches, otherwise throws a 400 on the "code" field.
     * Not rolled back on failure so wrong guesses are counted.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void consume(User user, EmailCodePurpose purpose, String raw) {
        EmailCode code = repository.findByUserAndPurpose(user, purpose).orElseThrow(EmailCodeService::invalid);
        if (!code.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            repository.delete(code);
            throw invalid();
        }
        if (raw == null || !passwordEncoder.matches(raw, code.getCodeHash())) {
            code.setAttempts(code.getAttempts() + 1);
            if (code.getAttempts() >= MAX_ATTEMPTS) {
                repository.delete(code);
                throw ApiException.invalidField("code", "Çok fazla hatalı deneme. Lütfen yeni kod isteyin.");
            }
            throw invalid();
        }
        repository.delete(code);
    }

    /** Thrown also for unknown e-mail addresses, so the response doesn't reveal them. */
    public static ApiException invalid() {
        return ApiException.invalidField("code", "Kod hatalı veya süresi dolmuş");
    }
}
