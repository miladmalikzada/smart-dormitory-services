package com.dorm.service;

import com.dorm.domain.Repository;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;

import java.util.List;

/** Full CRUD for students, with the rules that protect related data. */
public class StudentService {
    private final Repository<Student> students;
    private final Repository<Room> rooms;
    private final Repository<VisitorEntry> visitors;

    public StudentService(Repository<Student> students, Repository<Room> rooms, Repository<VisitorEntry> visitors) {
        this.students = students;
        this.rooms = rooms;
        this.visitors = visitors;
    }

    public Student register(String name, String phone) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        Student s = new Student(Ids.next("S"), name.trim(), phone);
        students.save(s);
        return s;
    }

    public List<Student> list() { return students.findAll(); }

    public Student get(String id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Unknown student " + id));
    }

    public Student update(String id, String name, String phone) {
        get(id);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        Student s = new Student(id, name.trim(), phone);
        students.save(s);
        return s;
    }

    public void delete(String id) {
        get(id);
        if (rooms.findAll().stream().anyMatch(r -> r.houses(id))) {
            throw new IllegalStateException("Student lives in a room; check them out first");
        }
        if (visitors.findAll().stream().anyMatch(v -> v.hostStudentId().equals(id))) {
            throw new IllegalStateException("Student has visitor records; delete those first");
        }
        students.delete(id);
    }
}
