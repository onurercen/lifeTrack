package com.lifetrack.common.security;

import com.lifetrack.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed password checks per key in a sliding window and refuses further
 * attempts with 429 once a key reaches its limit. In memory, so it is per instance
 * and resets on restart, which is fine for a single server.
 */
@Component
public class FailedAttemptLimiter {

    // Prune expired keys once the map grows past this, so it can't grow without bound.
    private static final int PRUNE_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration window;

    public FailedAttemptLimiter(Clock clock, @Value("${security.failed-attempts.window-minutes:15}") long windowMinutes) {
        this.clock = clock;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /** Throws 429 if [key] already failed [maxFailures] times within the window. */
    public void check(String key, int maxFailures) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return;
        }
        Instant now = clock.instant();
        synchronized (attempts) {
            prune(attempts, now);
            if (attempts.size() >= maxFailures) {
                Duration retryAfter = Duration.between(now, attempts.peekFirst().plus(window));
                throw ApiException.tooManyRequests(
                    "Çok fazla başarısız deneme. Lütfen " + minutes(retryAfter) + " dakika sonra tekrar deneyin.",
                    retryAfter
                );
            }
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        if (failures.size() > PRUNE_THRESHOLD) {
            pruneAll(now);
        }
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, now);
            attempts.addLast(now);
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> attempts, Instant now) {
        Instant cutoff = now.minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
            attempts.removeFirst();
        }
    }

    private void pruneAll(Instant now) {
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private static long minutes(Duration duration) {
        return Math.max(1, (duration.toSeconds() + 59) / 60);
    }
}
