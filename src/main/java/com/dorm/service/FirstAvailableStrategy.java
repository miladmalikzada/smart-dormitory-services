package com.dorm.service;

import com.dorm.domain.Room;

import java.util.List;
import java.util.Optional;

public class FirstAvailableStrategy implements RoomAllocationStrategy {
    @Override public Optional<Room> choose(List<Room> rooms) {
        return rooms.stream().filter(r -> !r.isFull()).findFirst();
    }
}
