package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
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
        auth.addAccount("admin", "correct-horse", Role.ADMIN);
    }

    @Test void goodPasswordGivesAWorkingToken() {
        AuthService.Login l = auth.login("Admin", "correct-horse");   // user names are not case sensitive
        assertEquals(Role.ADMIN, l.role());
        assertEquals(Role.ADMIN, auth.authenticate(l.token()).orElseThrow().role());
    }

    @Test void wrongPasswordAndUnknownUserGetTheSameMessage() {
        String a = assertThrows(AuthenticationException.class, () -> auth.login("admin", "nope")).getMessage();
        String b = assertThrows(AuthenticationException.class, () -> auth.login("ghost", "nope")).getMessage();
        assertEquals(a, b);
    }

    @Test void tokensExpireAndLogoutWorks() {
        String token = auth.login("admin", "correct-horse").token();
        clock.now = clock.now.plus(AuthService.TOKEN_LIFETIME).plusSeconds(1);
        assertTrue(auth.authenticate(token).isEmpty());
        String second = auth.login("admin", "correct-horse").token();
        auth.logout(second);
        assertTrue(auth.authenticate(second).isEmpty());
        assertTrue(auth.authenticate(null).isEmpty());
        assertTrue(auth.authenticate("made-up").isEmpty());
    }

    @Test void tooManyFailuresLockTheAccountForAMinute() {
        for (int i = 0; i < AuthService.MAX_FAILURES; i++) {
            assertThrows(AuthenticationException.class, () -> auth.login("admin", "bad"));
        }
        assertThrows(RateLimitedException.class, () -> auth.login("admin", "correct-horse"));   // even the right one
        clock.now = clock.now.plus(AuthService.LOCK_WINDOW).plus(Duration.ofSeconds(1));
        assertNotNull(auth.login("admin", "correct-horse"));
    }

    @Test void shortPasswordsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> auth.addAccount("x", "short", Role.STAFF));
        assertTrue(auth.hasAccounts());
    }
}
