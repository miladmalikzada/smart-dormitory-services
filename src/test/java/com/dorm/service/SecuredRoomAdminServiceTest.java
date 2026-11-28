package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Role;
import com.dorm.infra.InMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Protection Proxy: everybody may look at rooms, only an administrator may change them. */
class SecuredRoomAdminServiceTest {
    private SessionContext session;
    private SecuredRoomAdminService rooms;

    @BeforeEach void setUp() {
        session = new SessionContext(Role.ADMIN);
        rooms = new SecuredRoomAdminService(
                new RoomAdminService(new InMemoryRepository<>(), new InMemoryRepository<>()), session);
    }

    @Test void anAdministratorMayAddResizeAndDeleteRooms() {
        rooms.create("R301", 2);
        assertEquals(4, rooms.updateCapacity("R301", 4).capacity());
        rooms.delete("R301");
        assertTrue(rooms.list().isEmpty());
    }
}
