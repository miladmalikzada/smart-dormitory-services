package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.domain.Role;
import com.dorm.infra.EnvConfig;
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
        api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()), EnvConfig.of(Map.of())).api();
    }

    private Api.Response call(String method, String path, String body, Role role) {
        return api.handle(method, path, body, role);
    }

    private Api.Response call(String method, String path, String body) {
        return call(method, path, body, Role.STUDENT);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> obj(Api.Response r) { return (Map<String, Object>) r.body(); }

    private static final String[] IINS = {"040517501237", "031102600458", "050130500772", "991231300013", "040229500095"};
    private static final String[] DOBS = {"2004-05-17", "2003-11-02", "2005-01-30", "1999-12-31", "2004-02-29"};
    private int counter;

    /** Valid student JSON; each call uses a different IIN so the uniqueness rule is not triggered. */
    private String studentJson(String first, int n) {
        return "{\"firstName\":\"" + first + "\",\"lastName\":\"Test\",\"dateOfBirth\":\"" + DOBS[n]
                + "\",\"iin\":\"" + IINS[n] + "\",\"passportNumber\":\"N1234567" + n + "\",\"phone\":\"1\"}";
    }

    private String newStudent(String name) {
        return (String) obj(call("POST", "/api/students", studentJson(name, counter++), Role.ADMIN)).get("id");
    }

    @Test void studentCrud() {
        Api.Response created = call("POST", "/api/students", studentJson("Ayan", 0), Role.ADMIN);
        assertEquals(201, created.status());
        String id = (String) obj(created).get("id");

        assertEquals(1, ((List<?>) call("GET", "/api/students", null, Role.ADMIN).body()).size());
        assertEquals("Ayan Test", obj(call("GET", "/api/students/" + id, null, Role.ADMIN)).get("name"));
        // identity numbers left out of an update keep their old values
        Api.Response updated = call("PUT", "/api/students/" + id,
                "{\"firstName\":\"Ayan\",\"lastName\":\"Nur\",\"dateOfBirth\":\"2004-05-17\",\"faculty\":\"IS\"}", Role.ADMIN);
        assertEquals("Ayan Nur", obj(updated).get("name"));
        assertEquals(IINS[0], obj(updated).get("iin"));

        assertEquals(200, call("DELETE", "/api/students/" + id, null, Role.ADMIN).status());
        assertEquals(404, call("GET", "/api/students/" + id, null, Role.ADMIN).status());
    }

    @Test void studentValidation() {
        assertEquals(400, call("POST", "/api/students", "{\"firstName\":\"\"}", Role.ADMIN).status());
        String noIin = "{\"firstName\":\"A\",\"lastName\":\"B\",\"dateOfBirth\":\"2004-05-17\"}";
        assertEquals(400, call("POST", "/api/students", noIin, Role.ADMIN).status());
        String badCheck = "{\"firstName\":\"A\",\"lastName\":\"B\",\"dateOfBirth\":\"2004-05-17\",\"iin\":\"040517501238\"}";
        assertEquals(400, call("POST", "/api/students", badCheck, Role.ADMIN).status());
        String wrongDob = "{\"firstName\":\"A\",\"lastName\":\"B\",\"dateOfBirth\":\"2004-05-18\",\"iin\":\"040517501237\"}";
        assertEquals(400, call("POST", "/api/students", wrongDob, Role.ADMIN).status());
        String badEmail = "{\"firstName\":\"A\",\"lastName\":\"B\",\"dateOfBirth\":\"2004-05-17\",\"iin\":\"040517501237\",\"email\":\"nope\"}";
        assertEquals(400, call("POST", "/api/students", badEmail, Role.ADMIN).status());
    }

    @Test void duplicateIdentityNumbersAreRejected() {
        newStudent("Ayan");
        counter = 0;   // same IIN again
        assertEquals(409, call("POST", "/api/students", studentJson("Copy", 0), Role.ADMIN).status());
    }

    @Test void rolesSeeDifferentViewsOfStudents() {
        String id = newStudent("Ayan");
        assertEquals(IINS[0], obj(call("GET", "/api/students/" + id, null, Role.ADMIN)).get("iin"));
        Map<String, Object> staff = obj(call("GET", "/api/students/" + id, null, Role.STAFF));
        assertEquals("********1237", staff.get("iin"));
        assertEquals("*****5670", staff.get("passportNumber"));
        Map<String, Object> student = obj(call("GET", "/api/students/" + id, null, Role.STUDENT));
        assertNull(student.get("iin"));
        assertNull(student.get("phone"));
        assertEquals("Ayan Test", student.get("name"));
    }

    @Test void onlyAdminsChangeStudents() {
        assertEquals(403, call("POST", "/api/students", studentJson("Ayan", 0), Role.STAFF).status());
        assertEquals(403, call("POST", "/api/students", studentJson("Ayan", 0), Role.STUDENT).status());
        String id = newStudent("Ayan");
        assertEquals(403, call("DELETE", "/api/students/" + id, null, Role.STAFF).status());
        assertEquals(403, call("PUT", "/api/students/" + id, studentJson("X", 0), Role.STUDENT).status());
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
        assertEquals(409, call("DELETE", "/api/students/" + student, null, Role.ADMIN).status());   // still has a room

        assertEquals(200, call("DELETE", "/api/rooms/R9/occupants/" + student, null).status());
        assertEquals(200, call("DELETE", "/api/rooms/R9", null).status());
        assertEquals(200, call("DELETE", "/api/students/" + student, null, Role.ADMIN).status());
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

        assertEquals(409, call("DELETE", "/api/students/" + host, null, Role.ADMIN).status());      // has visitor records
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
        assertEquals(400, call("POST", "/api/students", "{bad json", Role.ADMIN).status());
        assertEquals(405, call("PATCH", "/api/students", null).status());
        assertEquals(404, call("GET", "/api/nothing", null).status());
        assertEquals(404, call("GET", "/other", null).status());
    }

    @Test void certificateAndExportEndpoints() {
        String id = newStudent("Ayan");
        assertEquals(409, call("GET", "/api/certificates/" + id + "/text", null, Role.STAFF).status());   // no room yet
        call("POST", "/api/room-requests", "{\"studentId\":\"" + id + "\"}");
        Map<String, Object> doc = obj(call("GET", "/api/certificates/" + id + "/html", null, Role.STAFF));
        assertEquals("certificate-" + id + ".html", doc.get("filename"));
        assertTrue(doc.get("content").toString().contains("ACCOMMODATION CERTIFICATE"));
        assertEquals(403, call("GET", "/api/certificates/" + id + "/text", null, Role.STUDENT).status());
        assertEquals(400, call("GET", "/api/certificates/" + id + "/pdf", null, Role.STAFF).status());
        assertEquals(404, call("GET", "/api/certificates/nobody/text", null, Role.STAFF).status());

        String adminCsv = obj(call("GET", "/api/export/students", null, Role.ADMIN)).get("content").toString();
        String staffCsv = obj(call("GET", "/api/export/students", null, Role.STAFF)).get("content").toString();
        assertTrue(adminCsv.contains(IINS[0]));
        assertFalse(staffCsv.contains(IINS[0]));          // staff export is masked
        assertTrue(staffCsv.contains("********1237"));
        assertEquals(403, call("GET", "/api/export/students", null, Role.STUDENT).status());
        assertEquals(404, call("GET", "/api/export/rooms", null, Role.ADMIN).status());
        assertEquals(405, call("POST", "/api/export/students", null, Role.ADMIN).status());
    }
}
