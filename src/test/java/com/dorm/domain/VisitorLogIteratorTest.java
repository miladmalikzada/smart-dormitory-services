package com.dorm.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class VisitorLogIteratorTest {
    private final LocalDateTime t0 = LocalDateTime.of(2026, 11, 1, 10, 0);

    private VisitorLog log() {
        VisitorEntry a = new VisitorEntry("V1", "Ali", "S1", t0);
        VisitorEntry b = new VisitorEntry("V2", "Bota", "S1", t0.plusMinutes(5));
        VisitorEntry c = new VisitorEntry("V3", "Chen", "S2", t0.plusMinutes(9));
        b.depart(t0.plusHours(1));
        return new VisitorLog(List.of(a, b, c));
    }

    @Test void insideSkipsVisitorsWhoLeft() {
        List<String> names = new ArrayList<>();
        for (VisitorEntry e : log().inside()) names.add(e.visitorName());
        assertEquals(List.of("Ali", "Chen"), names);
    }

    @Test void plainIterationReturnsEveryone() {
        int n = 0;
        for (VisitorEntry ignored : log()) n++;
        assertEquals(3, n);
    }

    @Test void iteratorEndsCleanly() {
        var it = new VisitorLog(List.of()).inside().iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }
}
