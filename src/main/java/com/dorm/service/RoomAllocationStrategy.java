package com.dorm.service;

import com.dorm.domain.Room;

import java.util.List;
import java.util.Optional;

/** Strategy pattern (L8): how to pick a room for a new resident. */
public interface RoomAllocationStrategy {
    Optional<Room> choose(List<Room> rooms);
}
