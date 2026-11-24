package com.dorm.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TicketStateTest {
    private MaintenanceTicket ticket() {
        return MaintenanceTicket.builder().id("T1").roomId("R1").description("Broken lamp").build();
    }

    @Test void followsTheHappyPath() {
        MaintenanceTicket t = ticket();
        assertEquals("OPEN", t.status());
        t.assignTo("Aibek");
        assertEquals("ASSIGNED", t.status());
        t.start();
        assertEquals("IN_PROGRESS", t.status());
        t.resolve();
        assertEquals("RESOLVED", t.status());
    }

    @Test void rejectsIllegalTransitions() {
        MaintenanceTicket t = ticket();
        assertThrows(IllegalStateException.class, t::start);
        assertThrows(IllegalStateException.class, t::resolve);
        t.assignTo("Aibek");
        assertThrows(IllegalStateException.class, () -> t.assignTo("Other"));
    }
}
