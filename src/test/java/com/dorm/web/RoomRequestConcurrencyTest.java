package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.domain.Iin;
import com.dorm.infra.EnvConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Many clients ask for a room at the same moment, over real HTTP. The server must never put more
 * residents in a room than it has beds, and nobody may end up in two rooms.
 * (The memory storage starts with three rooms: R101 and R102 with 2 beds, R201 with 3 beds = 7 beds.)
 */
class RoomRequestConcurrencyTest {
    private static final int BEDS = 7;
    @TempDir Path tmp;
    private Api api;
    private WebServer server;
    private String token;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach void setUp() throws Exception {
        EnvConfig cfg = EnvConfig.of(Map.of("DORM_ADMIN_PASSWORD", "admin-pass-1"));
        api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()), cfg).api();
        server = new WebServer(api, 0);
        server.start();
        Api.Response login = api.handleRequest("POST", "/api/login", "{\"username\":\"admin\",\"password\":\"admin-pass-1\"}", null);
        token = (String) ((Map<?, ?>) login.body()).get("token");
    }

    @AfterEach void tearDown() { server.stop(); }

    /** Valid IINs for people born on 2004-05-17, found by trying serial numbers until the check digit fits. */
    private static List<String> validIins(int count) {
        List<String> found = new ArrayList<>();
        for (int serial = 0; serial < 10000 && found.size() < count; serial++) {
            String start = "0405175" + String.format("%04d", serial);
            for (int check = 0; check < 10; check++) {
                if (Iin.isValid(start + check)) { found.add(start + check); break; }
            }
        }
        return found;
    }

    private String newStudent(String iin, String firstName) {
        String body = "{\"firstName\":\"" + firstName + "\",\"lastName\":\"Test\",\"dateOfBirth\":\"2004-05-17\",\"iin\":\"" + iin + "\"}";
        Api.Response r = api.handleRequest("POST", "/api/students", body, "Bearer " + token);
        assertEquals(201, r.status());
        return (String) ((Map<?, ?>) r.body()).get("id");
    }

    private int requestRoom(String studentId) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/api/room-requests"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"studentId\":\"" + studentId + "\"}")).build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    /** Sends all requests at the same moment and returns their status codes. */
    private List<Integer> requestAtOnce(List<String> studentIds) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(studentIds.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (String id : studentIds) {
                results.add(pool.submit(() -> { go.await(); return requestRoom(id); }));
            }
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) statuses.add(f.get());
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rooms() {
        return (List<Map<String, Object>>) api.handleRequest("GET", "/api/rooms", null, "Bearer " + token).body();
    }

    @Test void twelveStudentsAskingAtOnceNeverOverfillARoom() throws Exception {
        List<String> ids = new ArrayList<>();
        List<String> iins = validIins(12);
        for (int i = 0; i < iins.size(); i++) ids.add(newStudent(iins.get(i), "Student" + i));

        List<Integer> statuses = requestAtOnce(ids);

        assertEquals(BEDS, statuses.stream().filter(s -> s == 201).count(), "one room per free bed");
        assertEquals(ids.size() - BEDS, statuses.stream().filter(s -> s == 409).count(), "the rest are told no room is free");
        Set<Object> housed = new HashSet<>();
        int residents = 0;
        for (Map<String, Object> room : rooms()) {
            List<?> occupants = (List<?>) room.get("occupants");
            assertTrue(occupants.size() <= ((Number) room.get("capacity")).intValue(), "room " + room.get("id") + " is over capacity");
            residents += occupants.size();
            housed.addAll(occupants);
        }
        assertEquals(BEDS, residents);
        assertEquals(BEDS, housed.size(), "nobody lives in two rooms");
    }

    @Test void sixRequestsFromTheSameStudentGiveExactlyOneRoom() throws Exception {
        String id = newStudent(validIins(1).get(0), "Ayan");
        List<Integer> statuses = requestAtOnce(List.of(id, id, id, id, id, id));
        assertEquals(1, statuses.stream().filter(s -> s == 201).count());
        assertEquals(5, statuses.stream().filter(s -> s == 409).count());
        long roomsWithAyan = rooms().stream().filter(r -> ((List<?>) r.get("occupants")).contains(id)).count();
        assertEquals(1, roomsWithAyan);
    }
}
