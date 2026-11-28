package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.infra.EnvConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The real entry point of the web server: login, Bearer tokens, 401/403/429. */
class AuthApiTest {
    @TempDir Path tmp;
    private Api api;

    @BeforeEach void setUp() {
        EnvConfig cfg = EnvConfig.of(Map.of("DORM_ADMIN_PASSWORD", "admin-pass-1"));
        api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()), cfg).api();
    }

    @SuppressWarnings("unchecked")
    private String token(String user, String pass) {
        Api.Response r = api.handleRequest("POST", "/api/login", "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}", null);
        assertEquals(200, r.status());
        return (String) ((Map<String, Object>) r.body()).get("token");
    }

    private Api.Response signUp(String user, String pass, String role) {
        return api.handleRequest("POST", "/api/signup",
                "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\",\"role\":\"" + role + "\"}", null);
    }

    /** Signs up a staff or student account, lets the admin approve it, and returns its token. */
    private String account(String user, String role) {
        assertEquals(201, signUp(user, user + "-pass-1", role).status());
        if (!role.equals("STUDENT")) {
            assertEquals(200, api.handleRequest("POST", "/api/accounts/" + user + "/approve", null, "Bearer " + token("admin", "admin-pass-1")).status());
        }
        return token(user, user + "-pass-1");
    }

    private Api.Response get(String path, String token) {
        return api.handleRequest("GET", path, null, token == null ? null : "Bearer " + token);
    }

    @Test void everythingExceptLoginNeedsAToken() {
        assertEquals(401, get("/api/students", null).status());
        assertEquals(401, get("/api/summary", "forged-token").status());
        assertEquals(401, api.handleRequest("GET", "/api/rooms", null, "Basic abc").status());
    }

    @Test void loginFailuresAreCleanAndGeneric() {
        assertEquals(401, api.handleRequest("POST", "/api/login", "{\"username\":\"admin\",\"password\":\"bad\"}", null).status());
        assertEquals(401, api.handleRequest("POST", "/api/login", "{}", null).status());
        assertEquals(405, api.handleRequest("GET", "/api/login", null, null).status());
    }

    @Test void tokenRoleDecidesWhatYouSee() {
        String admin = token("admin", "admin-pass-1");
        String staff = account("maria", "STAFF");
        String student = account("aida", "STUDENT");
        String body = "{\"firstName\":\"Ayan\",\"lastName\":\"Nur\",\"dateOfBirth\":\"2004-05-17\",\"iin\":\"040517501237\"}";
        assertEquals(403, api.handleRequest("POST", "/api/students", body, "Bearer " + staff).status());
        Api.Response created = api.handleRequest("POST", "/api/students", body, "Bearer " + admin);
        assertEquals(201, created.status());
        String id = (String) ((Map<?, ?>) created.body()).get("id");
        assertEquals("040517501237", ((Map<?, ?>) get("/api/students/" + id, admin).body()).get("iin"));
        assertEquals("********1237", ((Map<?, ?>) get("/api/students/" + id, staff).body()).get("iin"));
        assertNull(((Map<?, ?>) get("/api/students/" + id, student).body()).get("iin"));
    }

    @Test void meAndLogout() {
        String t = account("maria", "STAFF");
        assertEquals("STAFF", ((Map<?, ?>) get("/api/me", t).body()).get("role"));
        assertFalse(((Map<?, ?>) get("/api/me", t).body()).containsKey("token"));
        assertEquals(200, api.handleRequest("POST", "/api/logout", null, "Bearer " + t).status());
        assertEquals(401, get("/api/me", t).status());
    }

    @Test void repeatedWrongPasswordsGet429() {
        for (int i = 0; i < 5; i++) {
            assertEquals(401, api.handleRequest("POST", "/api/login", "{\"username\":\"admin\",\"password\":\"x" + i + "\"}", null).status());
        }
        assertEquals(429, api.handleRequest("POST", "/api/login", "{\"username\":\"admin\",\"password\":\"admin-pass-1\"}", null).status());
    }

    // ---- sign-up and approval ----

    @SuppressWarnings("unchecked")
    @Test void signUpIsPublicAndStudentsAreActiveAtOnce() {
        Api.Response r = signUp("Aida", "aida-pass-1", "student");
        assertEquals(201, r.status());
        Map<String, Object> body = (Map<String, Object>) r.body();
        assertEquals("ACTIVE", body.get("status"));
        assertEquals("aida", body.get("username"));
        assertFalse(body.containsKey("password") || body.containsKey("hash"));
        assertEquals("STUDENT", ((Map<?, ?>) get("/api/me", token("aida", "aida-pass-1")).body()).get("role"));
    }

    @Test void staffAndAdminSignUpsWaitAndCannotSignInYet() {
        assertEquals("PENDING", ((Map<?, ?>) signUp("maria", "maria-pass-1", "STAFF").body()).get("status"));
        assertEquals("PENDING", ((Map<?, ?>) signUp("boss2", "boss2-pass-1", "ADMIN").body()).get("status"));
        Api.Response login = api.handleRequest("POST", "/api/login", "{\"username\":\"maria\",\"password\":\"maria-pass-1\"}", null);
        assertEquals(401, login.status());
        assertTrue(String.valueOf(((Map<?, ?>) login.body()).get("error")).contains("waiting for approval"));
    }

    @Test void signUpRejectsBadInputAndDuplicates() {
        assertEquals(400, signUp("ab", "long-enough-1", "STUDENT").status());
        assertEquals(400, signUp("someone", "short", "STUDENT").status());
        assertEquals(400, signUp("someone", "long-enough-1", "SUPERUSER").status());
        assertEquals(400, api.handleRequest("POST", "/api/signup", "{\"username\":\"someone\",\"password\":\"long-enough-1\"}", null).status());
        assertEquals(201, signUp("someone", "long-enough-1", "STUDENT").status());
        assertEquals(409, signUp("someone", "long-enough-1", "STUDENT").status());
        assertEquals(409, signUp("admin", "long-enough-1", "ADMIN").status());   // cannot take over the owner's name
        assertEquals(405, api.handleRequest("GET", "/api/signup", null, null).status());
    }

    @SuppressWarnings("unchecked")
    @Test void onlyTheAdminCanListApproveAndRejectAccounts() {
        signUp("maria", "maria-pass-1", "STAFF");
        String admin = token("admin", "admin-pass-1");
        String student = account("aida", "STUDENT");

        assertEquals(401, get("/api/accounts", null).status());
        assertEquals(403, get("/api/accounts", student).status());
        assertEquals(403, api.handleRequest("POST", "/api/accounts/maria/approve", null, "Bearer " + student).status());

        java.util.List<Map<String, Object>> list = (java.util.List<Map<String, Object>>) get("/api/accounts", admin).body();
        assertEquals("maria", list.get(0).get("username"));           // waiting requests come first
        assertEquals("PENDING", list.get(0).get("status"));
        assertTrue(list.stream().noneMatch(a -> a.containsKey("hash") || a.containsKey("password") || a.containsKey("salt")));

        assertEquals("ACTIVE", ((Map<?, ?>) api.handleRequest("POST", "/api/accounts/maria/approve", null, "Bearer " + admin).body()).get("status"));
        assertEquals("STAFF", ((Map<?, ?>) get("/api/me", token("maria", "maria-pass-1")).body()).get("role"));
        assertEquals(404, api.handleRequest("POST", "/api/accounts/ghost/approve", null, "Bearer " + admin).status());
        assertEquals(400, api.handleRequest("POST", "/api/accounts/admin/reject", null, "Bearer " + admin).status());   // not yourself
        assertEquals(404, api.handleRequest("POST", "/api/accounts/maria/promote", null, "Bearer " + admin).status());
        assertEquals(405, api.handleRequest("DELETE", "/api/accounts", null, "Bearer " + admin).status());
    }

    @Test void switchingAnAccountOffEndsItsSessionImmediately() {
        String admin = token("admin", "admin-pass-1");
        String student = account("aida", "STUDENT");
        assertEquals(200, get("/api/summary", student).status());
        assertEquals(200, api.handleRequest("POST", "/api/accounts/aida/reject", null, "Bearer " + admin).status());
        assertEquals(401, get("/api/summary", student).status());
        assertEquals(401, api.handleRequest("POST", "/api/login", "{\"username\":\"aida\",\"password\":\"aida-pass-1\"}", null).status());
    }

    @Test void aSignedUpStudentCannotGiveThemselvesMorePower() {
        String student = account("aida", "STUDENT");
        // asking for ADMIN in a later request changes nothing: the role is fixed when the account is approved
        assertEquals(403, api.handleRequest("POST", "/api/accounts/aida/approve", null, "Bearer " + student).status());
        assertEquals("STUDENT", ((Map<?, ?>) get("/api/me", student).body()).get("role"));
    }

    @Test void oldStaffAndStudentPasswordsInEnvCreateNoAccounts() {
        EnvConfig cfg = EnvConfig.of(Map.of("DORM_ADMIN_PASSWORD", "admin-pass-1", "DORM_STAFF_PASSWORD", "staff-pass-1",
                "DORM_STUDENT_PASSWORD", "student-pass-1"));
        Api other = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()), cfg).api();
        assertEquals(401, other.handleRequest("POST", "/api/login", "{\"username\":\"staff\",\"password\":\"staff-pass-1\"}", null).status());
        assertEquals(401, other.handleRequest("POST", "/api/login", "{\"username\":\"student\",\"password\":\"student-pass-1\"}", null).status());
    }

    // ---- change password ----

    private Api.Response changePassword(String token, String oldPassword, String newPassword) {
        return api.handleRequest("POST", "/api/me/password",
                "{\"oldPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword + "\"}",
                token == null ? null : "Bearer " + token);
    }

    @Test void aUserCanChangeTheirOwnPassword() {
        String student = account("aida", "STUDENT");
        assertEquals(200, changePassword(student, "aida-pass-1", "aida-new-pass-2").status());
        assertEquals(200, get("/api/me", student).status());   // this session stays open
        assertNotNull(token("aida", "aida-new-pass-2"));
        assertEquals(401, api.handleRequest("POST", "/api/login", "{\"username\":\"aida\",\"password\":\"aida-pass-1\"}", null).status());
    }

    @Test void changePasswordGivesClearErrors() {
        String student = account("aida", "STUDENT");
        assertEquals(403, changePassword(student, "not-my-password", "aida-new-pass-2").status());   // wrong current password
        assertEquals(400, changePassword(student, "aida-pass-1", "short").status());
        assertEquals(400, changePassword(student, "aida-pass-1", "aida-pass-1").status());           // same as before
        assertEquals(200, get("/api/me", student).status());   // a wrong guess does not sign the user out
    }
}
