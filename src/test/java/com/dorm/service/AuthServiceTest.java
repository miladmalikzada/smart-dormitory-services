package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.AccountStatus;
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

    // ---- sign-up and approval ----

    @Test void studentsCanSignUpAndSignInAtOnce() {
        AccountView v = auth.signUp("Aida.K", "my-secret-1", Role.STUDENT);
        assertEquals("aida.k", v.username());
        assertEquals(AccountStatus.ACTIVE, v.status());
        assertEquals(Role.STUDENT, auth.login("AIDA.K", "my-secret-1").role());
    }

    @Test void staffAndAdminWaitForApproval() {
        for (Role role : new Role[] {Role.STAFF, Role.ADMIN}) {
            String name = "new-" + role.name().toLowerCase();
            assertEquals(AccountStatus.PENDING, auth.signUp(name, "my-secret-1", role).status());
            AuthenticationException e = assertThrows(AuthenticationException.class, () -> auth.login(name, "my-secret-1"));
            assertTrue(e.getMessage().contains("waiting for approval"));
            // a wrong password must not reveal that the account exists or is pending
            assertEquals("Invalid username or password", assertThrows(AuthenticationException.class, () -> auth.login(name, "wrong-pass")).getMessage());
        }
    }

    @Test void approvedStaffCanSignInWithTheRightRole() {
        auth.signUp("maria", "my-secret-1", Role.STAFF);
        assertEquals(AccountStatus.ACTIVE, auth.approve("admin", "maria").status());
        assertEquals(Role.STAFF, auth.login("maria", "my-secret-1").role());
    }

    @Test void rejectedAccountsCannotSignInAndOpenSessionsEnd() {
        auth.signUp("bob", "my-secret-1", Role.STUDENT);
        String token = auth.login("bob", "my-secret-1").token();
        auth.reject("admin", "bob");
        assertTrue(auth.authenticate(token).isEmpty());
        assertTrue(assertThrows(AuthenticationException.class, () -> auth.login("bob", "my-secret-1")).getMessage().contains("not approved"));
        auth.approve("admin", "bob");   // can be switched on again
        assertNotNull(auth.login("bob", "my-secret-1"));
    }

    @Test void anAdministratorCannotChangeTheirOwnAccount() {
        assertThrows(IllegalArgumentException.class, () -> auth.reject("admin", "admin"));
        assertThrows(IllegalArgumentException.class, () -> auth.approve("Admin", "admin"));
    }

    @Test void unknownAccountsAreNotFound() {
        assertThrows(NotFoundException.class, () -> auth.approve("admin", "nobody"));
    }

    @Test void signUpChecksUsernamePasswordAndDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("ab", "my-secret-1", Role.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("has space", "my-secret-1", Role.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("valid.name", "short", Role.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("valid.name", "x".repeat(129), Role.STUDENT));
        assertThrows(IllegalArgumentException.class, () -> auth.signUp("valid.name", "my-secret-1", null));
        auth.signUp("taken", "my-secret-1", Role.STUDENT);
        assertThrows(IllegalStateException.class, () -> auth.signUp("TAKEN", "other-secret-1", Role.STUDENT));
        assertThrows(IllegalStateException.class, () -> auth.signUp("admin", "other-secret-1", Role.ADMIN));   // the owner's name is taken
    }

    @Test void signUpIsLimitedPerMinuteAndPendingRequestsAreCapped() {
        for (int i = 0; i < AuthService.MAX_SIGNUPS_PER_MINUTE; i++) auth.signUp("user" + i, "my-secret-1", Role.STUDENT);
        assertThrows(RateLimitedException.class, () -> auth.signUp("one-too-many", "my-secret-1", Role.STUDENT));
        clock.now = clock.now.plus(AuthService.LOCK_WINDOW).plusSeconds(1);
        for (int i = 0; i < AuthService.MAX_PENDING; i++) {
            if (i % AuthService.MAX_SIGNUPS_PER_MINUTE == 0) clock.now = clock.now.plus(AuthService.LOCK_WINDOW).plusSeconds(1);
            auth.signUp("staff" + i, "my-secret-1", Role.STAFF);
        }
        clock.now = clock.now.plus(AuthService.LOCK_WINDOW).plusSeconds(1);
        assertThrows(RateLimitedException.class, () -> auth.signUp("staff-extra", "my-secret-1", Role.STAFF));
        assertNotNull(auth.signUp("student-extra", "my-secret-1", Role.STUDENT));   // students are never blocked by the cap
    }

    @Test void listShowsWaitingRequestsFirstAndNeverExposesHashes() {
        auth.signUp("zed", "my-secret-1", Role.STUDENT);
        clock.now = clock.now.plusSeconds(5);
        auth.signUp("amy", "my-secret-1", Role.STAFF);
        java.util.List<AccountView> list = auth.listAccounts();
        assertEquals("amy", list.get(0).username());
        assertEquals(AccountStatus.PENDING, list.get(0).status());
        assertEquals(3, list.size());
        assertFalse(list.get(0).toString().toLowerCase().contains("hash"));
    }

    @Test void resettingTheOwnerAccountAlwaysMakesItActiveAgain() {
        auth.signUp("owner2", "my-secret-1", Role.STAFF);
        auth.addAccount("owner2", "fresh-secret-1", Role.ADMIN);   // what App does for the .env admin at every start
        assertEquals(Role.ADMIN, auth.login("owner2", "fresh-secret-1").role());
    }

    @Test void changedPasswordWorksAndTheOldOneStops() {
        String token = auth.login("admin", "correct-horse").token();
        auth.changePassword("admin", token, "correct-horse", "brand-new-pass");
        assertNotNull(auth.login("admin", "brand-new-pass"));
        assertThrows(AuthenticationException.class, () -> auth.login("admin", "correct-horse"));
    }
}
