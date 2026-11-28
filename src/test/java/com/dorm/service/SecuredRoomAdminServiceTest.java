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

    @Test void staffMayLookAtRoomsButNotChangeThem() {
        rooms.create("R301", 2);
        session.switchTo(Role.STAFF);
        assertEquals(1, rooms.list().size());
        assertEquals("R301", rooms.get("R301").id());
        assertThrows(AccessDeniedException.class, () -> rooms.create("R302", 2));
        assertThrows(AccessDeniedException.class, () -> rooms.updateCapacity("R301", 5));
        assertThrows(AccessDeniedException.class, () -> rooms.delete("R301"));
        assertEquals(2, rooms.get("R301").capacity());   // nothing changed
    }

    @Test void studentsMayLookAtRoomsButNotChangeThem() {
        rooms.create("R301", 2);
        session.switchTo(Role.STUDENT);
        assertEquals(1, rooms.list().size());
        assertThrows(AccessDeniedException.class, () -> rooms.create("R302", 2));
        assertThrows(AccessDeniedException.class, () -> rooms.delete("R301"));
        assertEquals(1, rooms.list().size());
    }

    @Test void theProxyFollowsTheRoleOfTheCurrentSession() {
        session.switchTo(Role.STUDENT);
        assertThrows(AccessDeniedException.class, () -> rooms.create("R301", 2));
        session.switchTo(Role.ADMIN);
        assertEquals("R301", rooms.create("R301", 2).id());
    }
}
