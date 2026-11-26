package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.domain.Role;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests every REST resource through Api.handle: no network and no database server needed. */
class ApiTest {
    @TempDir Path tmp;
    private Api api;

    @BeforeEach void setUp() {
        api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream())).api();
    }

    private Api.Response call(String method, String path, String body, Role role) {
        return api.handle(method, path, body, role);
    }

    private Api.Response call(String method, String path, String body) {
        return call(method, path, body, Role.STUDENT);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> obj(Api.Response r) { return (Map<String, Object>) r.body(); }

    private String newStudent(String name) {
        return (String) obj(call("POST", "/api/students", "{\"name\":\"" + name + "\",\"phone\":\"1\"}")).get("id");
    }

    @Test void studentCrud() {
        Api.Response created = call("POST", "/api/students", "{\"name\":\"Ayan\",\"phone\":\"777\"}");
        assertEquals(201, created.status());
        String id = (String) obj(created).get("id");

        assertEquals(1, ((List<?>) call("GET", "/api/students", null).body()).size());
        assertEquals("Ayan", obj(call("GET", "/api/students/" + id, null)).get("name"));
        assertEquals("Ayan N.", obj(call("PUT", "/api/students/" + id, "{\"name\":\"Ayan N.\",\"phone\":\"2\"}")).get("name"));

        assertEquals(200, call("DELETE", "/api/students/" + id, null).status());
        assertEquals(404, call("GET", "/api/students/" + id, null).status());
    }

    @Test void studentNameIsRequired() {
        assertEquals(400, call("POST", "/api/students", "{\"name\":\"\"}").status());
    }

    @Test void roomCrudAndSafetyRules() {
        assertEquals(201, call("POST", "/api/rooms", "{\"id\":\"R9\",\"capacity\":9}").status());
        assertEquals(409, call("POST", "/api/rooms", "{\"id\":\"R9\",\"capacity\":2}").status());
        assertEquals(10, ((Number) obj(call("PUT", "/api/rooms/R9", "{\"capacity\":10}")).get("capacity")).intValue());

        String student = newStudent("Dana");
        call("POST", "/api/room-requests", "{\"studentId\":\"" + student + "\"}");   // strategy picks R9 (most free beds)
        assertEquals(List.of(student), obj(call("GET", "/api/rooms/R9", null)).get("occupants"));

        assertEquals(409, call("DELETE", "/api/rooms/R9", null).status());              // not empty
        assertEquals(409, call("PUT", "/api/rooms/R9", "{\"capacity\":0}").status());   // smaller than residents
        assertEquals(409, call("DELETE", "/api/students/" + student, null).status());   // still has a room

        assertEquals(200, call("DELETE", "/api/rooms/R9/occupants/" + student, null).status());
        assertEquals(200, call("DELETE", "/api/rooms/R9", null).status());
        assertEquals(200, call("DELETE", "/api/students/" + student, null).status());
    }

    @Test void checkingOutFromTheWrongRoomChangesNothing() {
        String student = newStudent("Dana");
        call("POST", "/api/room-requests", "{\"studentId\":\"" + student + "\"}");
        assertEquals(409, call("DELETE", "/api/rooms/R101/occupants/" + student, null).status());
        boolean stillHoused = ((List<?>) call("GET", "/api/rooms", null).body()).stream()
                .anyMatch(r -> ((List<?>) ((Map<?, ?>) r).get("occupants")).contains(student));
        assertTrue(stillHoused);
    }

    @Test void roomRequestAndUndoRestoreTheExactState() {
        String student = newStudent("Ayan");
        Object before = call("GET", "/api/rooms", null).body().toString();
        assertEquals(201, call("POST", "/api/room-requests", "{\"studentId\":\"" + student + "\"}").status());
        assertEquals(409, call("POST", "/api/room-requests", "{\"studentId\":\"" + student + "\"}").status());
        assertEquals(200, call("POST", "/api/room-requests/undo", null).status());
        assertEquals(before, call("GET", "/api/rooms", null).body().toString());
        assertEquals(409, call("POST", "/api/room-requests/undo", null).status());     // nothing left to undo
    }

    @Test void ticketLifecycleAndRoles() {
        Api.Response created = call("POST", "/api/tickets", "{\"roomId\":\"R101\",\"description\":\"Heater\",\"priority\":\"high\"}");
        assertEquals(201, created.status());
        assertEquals("HIGH", obj(created).get("priority"));
        String id = (String) obj(created).get("id");

        assertEquals(403, call("PUT", "/api/tickets/" + id, "{\"action\":\"assign\",\"technician\":\"Aibek\"}").status());  // proxy
        assertEquals(409, call("PUT", "/api/tickets/" + id, "{\"action\":\"start\"}", Role.STAFF).status());               // state
        assertEquals("ASSIGNED", obj(call("PUT", "/api/tickets/" + id, "{\"action\":\"assign\",\"technician\":\"Aibek\"}", Role.STAFF)).get("status"));
        assertEquals("IN_PROGRESS", obj(call("PUT", "/api/tickets/" + id, "{\"action\":\"start\"}", Role.STAFF)).get("status"));
        assertEquals("RESOLVED", obj(call("PUT", "/api/tickets/" + id, "{\"action\":\"resolve\"}", Role.STAFF)).get("status"));

        assertEquals(403, call("DELETE", "/api/tickets/" + id, null).status());
        assertEquals(200, call("DELETE", "/api/tickets/" + id, null, Role.ADMIN).status());
        assertEquals(404, call("GET", "/api/tickets/" + id, null).status());
    }

    @Test void ticketValidation() {
        assertEquals(404, call("POST", "/api/tickets", "{\"roomId\":\"NOPE\",\"description\":\"x\"}").status());
        assertEquals(400, call("POST", "/api/tickets", "{\"roomId\":\"R101\",\"description\":\"x\",\"priority\":\"BAD\"}").status());
        assertEquals(400, call("PUT", "/api/tickets/T-1", "{}", Role.STAFF).status());
    }

    @Test void visitorFlow() {
        String host = newStudent("Ayan");
        Api.Response arrived = call("POST", "/api/visitors", "{\"visitorName\":\"Ali\",\"hostStudentId\":\"" + host + "\"}");
        assertEquals(201, arrived.status());
        String id = (String) obj(arrived).get("id");

        assertEquals(1, ((List<?>) call("GET", "/api/visitors/inside", null).body()).size());
        assertEquals(Boolean.FALSE, obj(call("PUT", "/api/visitors/" + id, null)).get("inside"));
        assertEquals(0, ((List<?>) call("GET", "/api/visitors/inside", null).body()).size());
        assertEquals(409, call("PUT", "/api/visitors/" + id, null).status());           // already left

        assertEquals(409, call("DELETE", "/api/students/" + host, null).status());      // has visitor records
        assertEquals(200, call("DELETE", "/api/visitors/" + id, null).status());
        assertEquals(404, call("POST", "/api/visitors", "{\"visitorName\":\"Ali\",\"hostStudentId\":\"nobody\"}").status());
    }

    @Test void announcementCrud() {
        Api.Response created = call("POST", "/api/announcements", "{\"title\":\"Water\",\"body\":\"Off at 10\"}");
        assertEquals(201, created.status());
        String id = (String) obj(created).get("id");
        assertEquals("Off at 11", obj(call("PUT", "/api/announcements/" + id, "{\"title\":\"Water\",\"body\":\"Off at 11\"}")).get("body"));
        assertEquals(1, ((List<?>) call("GET", "/api/announcements", null).body()).size());
        assertEquals(200, call("DELETE", "/api/announcements/" + id, null).status());
        assertEquals(404, call("GET", "/api/announcements/" + id, null).status());
    }

    @Test void summaryAndEventsReflectActivity() {
        newStudent("Ayan");
        call("POST", "/api/announcements", "{\"title\":\"Hi\",\"body\":\"x\"}");
        Map<String, Object> s = obj(call("GET", "/api/summary", null));
        assertEquals(1, ((Number) s.get("students")).intValue());
        assertEquals(3, ((Number) s.get("rooms")).intValue());
        assertFalse(((List<?>) call("GET", "/api/events", null).body()).isEmpty());
    }

    @Test void badRequestsGetCleanErrors() {
        assertEquals(400, call("POST", "/api/students", "{bad json").status());
        assertEquals(405, call("PATCH", "/api/students", null).status());
        assertEquals(404, call("GET", "/api/nothing", null).status());
        assertEquals(404, call("GET", "/other", null).status());
    }
}
