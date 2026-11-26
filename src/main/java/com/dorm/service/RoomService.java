package com.dorm.service;

import com.dorm.domain.Repository;
import com.dorm.domain.Room;
import com.dorm.domain.Student;

import java.util.List;
import java.util.Optional;

public class RoomService {
    private final Repository<Student> students;
    private final Repository<Room> rooms;
    private final RoomAllocationStrategy strategy;
    private final CommandHistory history;
    private final EventPublisher events;

    public RoomService(Repository<Student> students, Repository<Room> rooms,
                       RoomAllocationStrategy strategy, CommandHistory history, EventPublisher events) {
        this.students = students;
        this.rooms = rooms;
        this.strategy = strategy;
        this.history = history;
        this.events = events;
    }

    public Student registerStudent(String name, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        Student s = new Student(Ids.next("S"), name, phone);
        students.save(s);
        return s;
    }

    public Room addRoom(String id, int capacity) {
        Room r = new Room(id, capacity);
        rooms.save(r);
        return r;
    }

    /** Room request: picks a room with the strategy and runs an undoable command. */
    public Room requestRoom(String studentId) {
        students.findById(studentId).orElseThrow(() -> new IllegalArgumentException("Unknown student " + studentId));
        boolean housed = rooms.findAll().stream().anyMatch(r -> r.houses(studentId));
        if (housed) throw new IllegalStateException(studentId + " already has a room");
        Room chosen = strategy.choose(rooms.findAll())
                .orElseThrow(() -> new IllegalStateException("No free room available"));
        history.execute(new AssignRoomCommand(rooms, events, chosen.id(), studentId));
        return rooms.findById(chosen.id()).orElseThrow();
    }

    public Optional<String> undoLast() { return history.undoLast(); }

    public List<Room> rooms() { return rooms.findAll(); }
    public List<Student> students() { return students.findAll(); }
}
