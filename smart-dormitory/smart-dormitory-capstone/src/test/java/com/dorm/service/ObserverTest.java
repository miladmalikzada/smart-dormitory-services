package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.DormEvent;
import com.dorm.infra.InMemoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ObserverTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC);

    @Test void announcementReachesEveryListener() {
        EventPublisher events = new EventPublisher();
        AuditLogListener audit = new AuditLogListener();
        events.subscribe(audit);
        List<String> sent = new ArrayList<>();
        events.subscribe(new NotificationListener(
                List.of(new RecordingChannel(sent)), Set.of("ANNOUNCEMENT_POSTED")));

        new AnnouncementService(new InMemoryRepository<>(), events, clock).post("Water", "Off at 10:00");

        assertEquals(1, audit.events().size());
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("Off at 10:00"));
    }

    @Test void listenerIgnoresEventsItWasNotAskedAbout() {
        List<String> sent = new ArrayList<>();
        NotificationListener l = new NotificationListener(List.of(new RecordingChannel(sent)), Set.of("X"));
        l.onEvent(new DormEvent("Y", "ignored"));
        assertTrue(sent.isEmpty());
    }

    static class RecordingChannel implements NotificationChannel {
        private final List<String> sink;
        RecordingChannel(List<String> sink) { this.sink = sink; }
        public String name() { return "test"; }
        public void send(String recipient, String message) { sink.add(message); }
    }
}
