package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Room;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllocationStrategyTest {
    private List<Room> rooms() {
        Room full = new Room("A", 1);
        full.assign("x");
        Room half = new Room("B", 3);
        half.assign("y");
        Room empty = new Room("C", 3);
        return List.of(full, half, empty);
    }

    @Test void firstAvailableSkipsFullRooms() {
        assertEquals("B", new FirstAvailableStrategy().choose(rooms()).orElseThrow().id());
    }

    @Test void leastOccupiedPicksRoomWithMostFreeBeds() {
        assertEquals("C", new LeastOccupiedStrategy().choose(rooms()).orElseThrow().id());
    }

    @Test void bothReturnEmptyWhenEverythingIsFull() {
        Room full = new Room("A", 1);
        full.assign("x");
        assertTrue(new FirstAvailableStrategy().choose(List.of(full)).isEmpty());
        assertTrue(new LeastOccupiedStrategy().choose(List.of(full)).isEmpty());
    }
}
