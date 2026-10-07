package com.lifetrack.common.security;

import com.lifetrack.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailedAttemptLimiterTest {

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-07T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final FailedAttemptLimiter limiter = new FailedAttemptLimiter(clock, 15);

    @Test
    void blocksAtLimitWithRetryAfterUntilOldestFailureLeavesTheWindow() {
        limiter.recordFailure("k");
        clock.advance(Duration.ofMinutes(5));
        limiter.recordFailure("k");
        assertThatCode(() -> limiter.check("k", 3)).doesNotThrowAnyException();

        limiter.recordFailure("k");
        assertThatThrownBy(() -> limiter.check("k", 3))
            .isInstanceOfSatisfying(ApiException.class, ex -> {
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMinutes(10));
                assertThat(ex.getMessage()).contains("10 dakika");
            });

        // The first failure expires after 15 minutes, freeing one attempt.
        clock.advance(Duration.ofMinutes(10));
        assertThatCode(() -> limiter.check("k", 3)).doesNotThrowAnyException();
    }

    @Test
    void keysAreIndependentAndResetClearsAKey() {
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        assertThatThrownBy(() -> limiter.check("a", 2)).isInstanceOf(ApiException.class);
        assertThatCode(() -> limiter.check("b", 2)).doesNotThrowAnyException();

        limiter.reset("a");
        assertThatCode(() -> limiter.check("a", 2)).doesNotThrowAnyException();
    }
}
