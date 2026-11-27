package com.dorm.service;

import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Repository;
import com.dorm.domain.Room;

import java.util.List;

/** Full CRUD for rooms (creating, resizing, deleting), with safety rules. */
public class RoomAdminService {
    private final Repository<Room> rooms;
    private final Repository<MaintenanceTicket> tickets;

    public RoomAdminService(Repository<Room> rooms, Repository<MaintenanceTicket> tickets) {
        this.rooms = rooms;
        this.tickets = tickets;
    }

    public List<Room> list() { return rooms.findAll(); }

    public Room get(String id) {
        return rooms.findById(id).orElseThrow(() -> new NotFoundException("Unknown room " + id));
    }

    public Room create(String id, int capacity) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("room id required");
        if (rooms.findById(id.trim()).isPresent()) throw new IllegalStateException("Room " + id + " already exists");
        Room r = new Room(id.trim(), capacity);
        rooms.save(r);
        return r;
    }

    public Room updateCapacity(String id, int capacity) {
        Room old = get(id);
        if (capacity < old.occupants().size()) {
            throw new IllegalStateException("Room has " + old.occupants().size() + " residents; capacity cannot be smaller");
        }
        Room resized = new Room(id, capacity);
        old.occupants().forEach(resized::assign);
        rooms.save(resized);
        return resized;
    }

    public void delete(String id) {
        Room r = get(id);
        if (!r.occupants().isEmpty()) throw new IllegalStateException("Room is not empty");
        if (tickets.findAll().stream().anyMatch(t -> t.roomId().equals(id))) {
            throw new IllegalStateException("Room has maintenance tickets; delete those first");
        }
        rooms.delete(id);
    }
}
