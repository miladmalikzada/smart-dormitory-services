package com.dorm.service;

import com.dorm.domain.Room;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy pattern (L8): fills one floor before moving to the next.
 * The floor is read from the room id: R101 and R102 are on floor 1, R201 on floor 2.
 * Inside a floor the room with the smallest id is used first.
 */
public class FloorStrategy implements RoomAllocationStrategy {
    @Override public Optional<Room> choose(List<Room> rooms) {
        return rooms.stream().filter(r -> !r.isFull())
                .min(Comparator.comparingInt((Room r) -> floorOf(r.id())).thenComparing(Room::id));
    }

    /** Digits of the id divided by 100 (R204 is floor 2). Ids with fewer than three digits count as floor 0. */
    static int floorOf(String roomId) {
        String digits = roomId.replaceAll("\\D", "");
        if (digits.length() > 9) digits = digits.substring(0, 9);
        return digits.isEmpty() ? 0 : Integer.parseInt(digits) / 100;
    }
}
