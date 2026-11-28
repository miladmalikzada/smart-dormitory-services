package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Room;
import java.util.List;
import org.junit.jupiter.api.Test;

class FloorStrategyTest {
    private static Room room(String id, int capacity, int taken) {
        Room r = new Room(id, capacity);
        for (int i = 0; i < taken; i++) r.assign(id + "-s" + i);
        return r;
    }

    @Test void emptyListOfRoomsGivesNoRoom() {
        assertTrue(new FloorStrategy().choose(List.of()).isEmpty());
    }

    @Test void fullDormitoryGivesNoRoom() {
        assertTrue(new FloorStrategy().choose(List.of(room("R101", 1, 1), room("R201", 2, 2))).isEmpty());
    }

    @Test void lowestFloorIsFilledFirst() {
        List<Room> rooms = List.of(room("R201", 3, 0), room("R101", 3, 1));
        assertEquals("R101", new FloorStrategy().choose(rooms).orElseThrow().id());
    }
}
