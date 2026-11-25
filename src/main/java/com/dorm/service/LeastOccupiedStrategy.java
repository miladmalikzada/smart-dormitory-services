package com.dorm.service;

import com.dorm.domain.Room;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Spreads residents: chooses the room with the most free beds (first one wins a tie). */
public class LeastOccupiedStrategy implements RoomAllocationStrategy {
    @Override public Optional<Room> choose(List<Room> rooms) {
        return rooms.stream().filter(r -> !r.isFull()).max(Comparator.comparingInt(Room::freeBeds));
    }
}
