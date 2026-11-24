package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Room;
import com.dorm.infra.InMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Command + Memento: undo must return the room to exactly the earlier state. */
class RoomUndoTest {
    private InMemoryRepository<Room> rooms;
    private RoomService service;

    @BeforeEach void setUp() {
        rooms = new InMemoryRepository<>();
        service = new RoomService(new InMemoryRepository<>(), rooms, new FirstAvailableStrategy(),
                new CommandHistory(), new EventPublisher());
        service.addRoom("R1", 2);
    }

    @Test void undoRestoresExactState() {
        String s1 = service.registerStudent("Ayan", "1").id();
        String s2 = service.registerStudent("Dana", "2").id();
        service.requestRoom(s1);
        var before = rooms.findById("R1").orElseThrow().occupants();

        service.requestRoom(s2);
        assertEquals(2, rooms.findById("R1").orElseThrow().occupants().size());

        assertTrue(service.undoLast().isPresent());
        assertEquals(before, rooms.findById("R1").orElseThrow().occupants());
    }

    @Test void undoTwiceEmptiesTheRoomAndThenReportsNothingToUndo() {
        service.requestRoom(service.registerStudent("Ayan", "1").id());
        assertTrue(service.undoLast().isPresent());
        assertTrue(rooms.findById("R1").orElseThrow().occupants().isEmpty());
        assertTrue(service.undoLast().isEmpty());
    }

    @Test void studentCannotHaveTwoRooms() {
        String s = service.registerStudent("Ayan", "1").id();
        service.requestRoom(s);
        assertThrows(IllegalStateException.class, () -> service.requestRoom(s));
    }

    @Test void failsWhenNoRoomIsFree() {
        service.requestRoom(service.registerStudent("A", "1").id());
        service.requestRoom(service.registerStudent("B", "2").id());
        String third = service.registerStudent("C", "3").id();
        assertThrows(IllegalStateException.class, () -> service.requestRoom(third));
    }
}
