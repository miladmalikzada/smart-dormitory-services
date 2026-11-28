package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.domain.Role;
import com.dorm.infra.EnvConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Bad input must give a clear 4xx answer and never leak a stack trace. */
class ApiValidationTest {
    @TempDir Path tmp;
    private Api api;

    @BeforeEach void setUp() {
        api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()), EnvConfig.of(Map.of())).api();
    }

    private static void assertCleanError(Api.Response r) {
        String text = String.valueOf(r.body());
        assertFalse(text.contains("Exception"), text);
        assertFalse(text.contains("at com.dorm"), text);
    }

    @Test void emptyBodyIsRejected() {
        Api.Response r = api.handle("POST", "/api/students", "", Role.ADMIN);
        assertEquals(400, r.status());
        assertCleanError(r);
    }

    @Test void brokenJsonIsRejected() {
        Api.Response r = api.handle("POST", "/api/students", "{not json", Role.ADMIN);
        assertEquals(400, r.status());
        assertCleanError(r);
    }

    @Test void wrongTypesAreRejected() {
        Api.Response r = api.handle("POST", "/api/students",
                "{\"firstName\":123,\"lastName\":true,\"dateOfBirth\":5,\"iin\":[1]}", Role.ADMIN);
        assertEquals(400, r.status());
        assertCleanError(r);
    }

    @Test void unknownIdIsNotFound() {
        Api.Response r = api.handle("GET", "/api/students/does-not-exist", null, Role.ADMIN);
        assertEquals(404, r.status());
        assertCleanError(r);
    }
}
