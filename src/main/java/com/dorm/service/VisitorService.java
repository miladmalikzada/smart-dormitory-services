package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.Repository;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.domain.VisitorLog;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;

public class VisitorService {
    private final Repository<VisitorEntry> entries;
    private final Repository<Student> students;
    private final EventPublisher events;
    private final Clock clock;

    public VisitorService(Repository<VisitorEntry> entries, Repository<Student> students,
                          EventPublisher events, Clock clock) {
        this.entries = entries;
        this.students = students;
        this.events = events;
        this.clock = clock;
    }

    public VisitorEntry arrive(String visitorName, String hostStudentId) {
        if (visitorName == null || visitorName.isBlank()) throw new IllegalArgumentException("visitor name required");
        students.findById(hostStudentId).orElseThrow(() -> new NotFoundException("Unknown host " + hostStudentId));
        VisitorEntry e = new VisitorEntry(Ids.next("V"), visitorName, hostStudentId, LocalDateTime.now(clock));
        entries.save(e);
        events.publish(new DormEvent("VISITOR_ARRIVED", visitorName + " visits " + hostStudentId));
        return e;
    }

    public void depart(String entryId) {
        VisitorEntry e = entries.findById(entryId).orElseThrow(() -> new NotFoundException("Unknown entry " + entryId));
        e.depart(LocalDateTime.now(clock));
        entries.save(e);
        events.publish(new DormEvent("VISITOR_LEFT", e.visitorName() + " left"));
    }

    public VisitorEntry get(String id) {
        return entries.findById(id).orElseThrow(() -> new NotFoundException("Unknown entry " + id));
    }

    public void delete(String id) {
        get(id);
        entries.delete(id);
        events.publish(new DormEvent("VISITOR_DELETED", "Visitor record " + id + " deleted"));
    }

    public VisitorLog log() {
        return new VisitorLog(entries.findAll().stream().sorted(Comparator.comparing(VisitorEntry::arrivedAt)).toList());
    }
}
