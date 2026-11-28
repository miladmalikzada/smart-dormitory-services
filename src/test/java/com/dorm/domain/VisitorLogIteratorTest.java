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

    @Test void emptyLogHasNoVisitors() {
        VisitorLog empty = new VisitorLog(List.of());
        assertFalse(empty.iterator().hasNext());
        assertFalse(empty.inside().iterator().hasNext());
    }

    @Test void insideIsEmptyWhenEveryoneHasLeft() {
        VisitorEntry a = new VisitorEntry("V1", "Ali", "S1", t0);
        VisitorEntry b = new VisitorEntry("V2", "Bota", "S2", t0.plusMinutes(3));
        a.depart(t0.plusHours(1));
        b.depart(t0.plusHours(2));
        assertFalse(new VisitorLog(List.of(a, b)).inside().iterator().hasNext());
    }

    @Test void oneVisitorStillInsideIsReturned() {
        VisitorEntry a = new VisitorEntry("V1", "Ali", "S1", t0);
        List<String> names = new ArrayList<>();
        for (VisitorEntry e : new VisitorLog(List.of(a)).inside()) names.add(e.visitorName());
        assertEquals(List.of("Ali"), names);
    }

    @Test void insideCanBeUsedTwiceWithTheSameResult() {
        VisitorLog log = log();
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        for (VisitorEntry e : log.inside()) first.add(e.visitorName());
        for (VisitorEntry e : log.inside()) second.add(e.visitorName());
        assertEquals(first, second);
    }

    @Test void visitorsKeepTheirArrivalOrder() {
        List<String> ids = new ArrayList<>();
        for (VisitorEntry e : log()) ids.add(e.id());
        assertEquals(List.of("V1", "V2", "V3"), ids);
    }
}
