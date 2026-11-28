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

    @Test void movesToTheNextFloorWhenTheFloorIsFull() {
        List<Room> rooms = List.of(room("R101", 1, 1), room("R102", 2, 2), room("R201", 3, 0));
        assertEquals("R201", new FloorStrategy().choose(rooms).orElseThrow().id());
    }

    @Test void staysOnOneFloorUntilItIsFull() {
        // LeastOccupiedStrategy would pick R102 or R201 here (more free beds); the floor rule keeps using R101.
        List<Room> rooms = List.of(room("R101", 3, 2), room("R102", 3, 0), room("R201", 3, 0));
        assertEquals("R101", new FloorStrategy().choose(rooms).orElseThrow().id());
    }

    @Test void insideAFloorTheSmallestRoomIdIsUsedFirst() {
        List<Room> rooms = List.of(room("R103", 2, 0), room("R102", 2, 0));
        assertEquals("R102", new FloorStrategy().choose(rooms).orElseThrow().id());
    }

    @Test void floorIsReadFromTheRoomId() {
        assertEquals(1, FloorStrategy.floorOf("R101"));
        assertEquals(2, FloorStrategy.floorOf("R204"));
        assertEquals(0, FloorStrategy.floorOf("R12"));
        assertEquals(0, FloorStrategy.floorOf("A"));
    }
}
