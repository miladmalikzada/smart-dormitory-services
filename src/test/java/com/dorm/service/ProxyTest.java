package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.infra.InMemoryRepository;
import org.junit.jupiter.api.Test;

class ProxyTest {
    @Test void studentMayReportButNotChangeTickets() {
        InMemoryRepository<Room> rooms = new InMemoryRepository<>();
        rooms.save(new Room("R1", 2));
        SessionContext session = new SessionContext(Role.STUDENT);
        TicketOperations secured = new SecuredTicketService(
                new TicketService(new InMemoryRepository<MaintenanceTicket>(), rooms, new EventPublisher()), session);

        String id = secured.create("R1", "Heater broken", Priority.HIGH).id();
        assertThrows(AccessDeniedException.class, () -> secured.assign(id, "Aibek"));

        session.switchTo(Role.STAFF);
        secured.assign(id, "Aibek");
        secured.start(id);
        secured.resolve(id);
        assertEquals("RESOLVED", secured.list().get(0).status());
    }

    @Test void unknownRoomIsRejected() {
        TicketService s = new TicketService(new InMemoryRepository<>(), new InMemoryRepository<>(), new EventPublisher());
        assertThrows(IllegalArgumentException.class, () -> s.create("NOPE", "x", Priority.LOW));
    }
}
