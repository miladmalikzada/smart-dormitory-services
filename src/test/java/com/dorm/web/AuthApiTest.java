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
        EnvConfig cfg = EnvConfig.of(Map.of("DORM_ADMIN_PASSWORD", "admin-pass-1", "DORM_STAFF_PASSWORD", "staff-pass-1",
                "DORM_STUDENT_PASSWORD", "student-pass-1"));
        api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()), cfg).api();
    }

    @SuppressWarnings("unchecked")
    private String token(String user, String pass) {
        Api.Response r = api.handleRequest("POST", "/api/login", "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}", null);
        assertEquals(200, r.status());
        return (String) ((Map<String, Object>) r.body()).get("token");
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
        String staff = token("staff", "staff-pass-1");
        String student = token("student", "student-pass-1");
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
        String t = token("staff", "staff-pass-1");
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
}
