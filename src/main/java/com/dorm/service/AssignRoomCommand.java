package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.Repository;
import com.dorm.domain.Room;

/**
 * Combination of two patterns (L7): Command + Memento.
 * The command stores a Memento of the room before it changes it, so undo restores the exact state.
 */
public class AssignRoomCommand implements Command {
    private final Repository<Room> rooms;
    private final EventPublisher events;
    private final String roomId;
    private final String studentId;
    private Room.Memento before;

    public AssignRoomCommand(Repository<Room> rooms, EventPublisher events, String roomId, String studentId) {
        this.rooms = rooms;
        this.events = events;
        this.roomId = roomId;
        this.studentId = studentId;
    }

    @Override public void execute() {
        Room room = load();
        before = room.save();
        room.assign(studentId);
        rooms.save(room);
        events.publish(new DormEvent("ROOM_ASSIGNED", studentId + " moved into " + roomId));
    }

    @Override public void undo() {
        Room room = load();
        room.restore(before);
        rooms.save(room);
        events.publish(new DormEvent("ROOM_ASSIGNMENT_UNDONE", studentId + " left " + roomId));
    }

    @Override public String description() { return "assign " + studentId + " to " + roomId; }

    private Room load() {
        return rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Unknown room " + roomId));
    }
}
