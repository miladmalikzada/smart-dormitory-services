package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Room;
import com.dorm.infra.InMemoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The safety rules of room administration, without the access-control proxy. */
class RoomAdminServiceTest {
    private InMemoryRepository<MaintenanceTicket> tickets;
    private RoomAdminService admin;

    @BeforeEach void setUp() {
        tickets = new InMemoryRepository<>();
        admin = new RoomAdminService(new InMemoryRepository<>(), tickets);
    }

    @Test void creatingTheSameRoomTwiceIsRefused() {
        admin.create("R301", 2);
        assertThrows(IllegalStateException.class, () -> admin.create("R301", 3));
        assertEquals(2, admin.get("R301").capacity());
    }

    @Test void roomNumberMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> admin.create("", 2));
        assertThrows(IllegalArgumentException.class, () -> admin.create("   ", 2));
        assertThrows(IllegalArgumentException.class, () -> admin.create(null, 2));
        assertTrue(admin.list().isEmpty());
    }

    @Test void capacityBelowOneIsRefusedAndRoomNumbersAreTrimmed() {
        assertThrows(IllegalArgumentException.class, () -> admin.create("R301", 0));
        assertEquals("R302", admin.create("  R302 ", 2).id());
    }

    @Test void capacityCannotBeSmallerThanTheNumberOfResidents() {
        Room room = admin.create("R301", 3);
        room.assign("S1");
        room.assign("S2");
        assertThrows(IllegalStateException.class, () -> admin.updateCapacity("R301", 1));
        assertEquals(2, admin.updateCapacity("R301", 2).capacity());   // exactly the residents is fine
    }

    @Test void resizingKeepsTheResidents() {
        Room room = admin.create("R301", 2);
        room.assign("S1");
        Room bigger = admin.updateCapacity("R301", 5);
        assertEquals(List.of("S1"), bigger.occupants());
        assertEquals(4, bigger.freeBeds());
    }

    @Test void aRoomWithResidentsCannotBeDeleted() {
        admin.create("R301", 2).assign("S1");
        assertThrows(IllegalStateException.class, () -> admin.delete("R301"));
        assertEquals(1, admin.list().size());
    }
}
