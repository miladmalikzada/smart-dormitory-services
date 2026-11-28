package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Limits that protect the sign-up page: sign-ups per minute and requests waiting for approval. */
class AuthLimitsTest {
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-26T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private AuthService auth;

    @BeforeEach void setUp() {
        clock = new MutableClock();
        auth = new AuthService(clock);
    }

    private void minutePasses() { clock.now = clock.now.plus(AuthService.LOCK_WINDOW).plusSeconds(1); }

    /** Signs up {@code count} accounts with the given role, waiting a minute whenever the per-minute limit is reached. */
    private void signUpMany(String prefix, int count, Role role) {
        for (int i = 0; i < count; i++) {
            if (i > 0 && i % AuthService.MAX_SIGNUPS_PER_MINUTE == 0) minutePasses();
            auth.signUp(prefix + i, "long-enough-1", role);
        }
    }

    @Test void theTwentyFirstSignUpInAMinuteIsRefused() {
        signUpMany("student", AuthService.MAX_SIGNUPS_PER_MINUTE, Role.STUDENT);
        assertThrows(RateLimitedException.class, () -> auth.signUp("onemore", "long-enough-1", Role.STUDENT));
    }

    @Test void signUpsWorkAgainAfterTheMinute() {
        signUpMany("student", AuthService.MAX_SIGNUPS_PER_MINUTE, Role.STUDENT);
        minutePasses();
        assertEquals("onemore", auth.signUp("onemore", "long-enough-1", Role.STUDENT).username());
    }

    @Test void tooManyStaffRequestsAreRefusedButStudentsCanStillSignUp() {
        signUpMany("staff", AuthService.MAX_PENDING, Role.STAFF);
        minutePasses();
        assertThrows(RateLimitedException.class, () -> auth.signUp("staffextra", "long-enough-1", Role.STAFF));
        assertThrows(RateLimitedException.class, () -> auth.signUp("adminextra", "long-enough-1", Role.ADMIN));
        assertEquals("student1", auth.signUp("student1", "long-enough-1", Role.STUDENT).username());
    }

    @Test void decidingARequestFreesAPlaceInTheWaitingList() {
        signUpMany("staff", AuthService.MAX_PENDING, Role.STAFF);
        minutePasses();
        auth.approve("admin", "staff0");
        assertEquals("staffextra", auth.signUp("staffextra", "long-enough-1", Role.STAFF).username());
    }
}
