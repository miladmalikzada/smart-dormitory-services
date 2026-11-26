package com.dorm.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TicketBuilderTest {
    @Test void usesDefaultPriority() {
        MaintenanceTicket t = MaintenanceTicket.builder().id("T1").roomId("R1").description("Leak").build();
        assertEquals(Priority.MEDIUM, t.priority());
    }

    @Test void validatesRequiredFields() {
        assertThrows(IllegalStateException.class, () -> MaintenanceTicket.builder().id("T1").roomId("R1").build());
        assertThrows(IllegalStateException.class, () -> MaintenanceTicket.builder().description("x").build());
    }
}
