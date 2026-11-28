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
}
