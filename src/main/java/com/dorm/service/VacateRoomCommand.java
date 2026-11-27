package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.Repository;
import com.dorm.domain.Room;

/** Command + Memento again: checking a student out is undoable too. */
public class VacateRoomCommand implements Command {
    private final Repository<Room> rooms;
    private final EventPublisher events;
    private final String roomId;
    private final String studentId;
    private Room.Memento before;

    public VacateRoomCommand(Repository<Room> rooms, EventPublisher events, String roomId, String studentId) {
        this.rooms = rooms;
        this.events = events;
        this.roomId = roomId;
        this.studentId = studentId;
    }

    @Override public void execute() {
        Room room = load();
        before = room.save();
        room.vacate(studentId);
        rooms.save(room);
        events.publish(new DormEvent("ROOM_VACATED", studentId + " checked out of " + roomId));
    }

    @Override public void undo() {
        Room room = load();
        room.restore(before);
        rooms.save(room);
        events.publish(new DormEvent("ROOM_VACATE_UNDONE", studentId + " moved back into " + roomId));
    }

    @Override public String description() { return "check out " + studentId + " from " + roomId; }

    private Room load() {
        return rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Unknown room " + roomId));
    }
}
