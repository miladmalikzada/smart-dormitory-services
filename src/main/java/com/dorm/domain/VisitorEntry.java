package com.dorm.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public class VisitorEntry implements Entity, Serializable {
    private final String id;
    private final String visitorName;
    private final String hostStudentId;
    private final LocalDateTime arrivedAt;
    private LocalDateTime departedAt;

    public VisitorEntry(String id, String visitorName, String hostStudentId, LocalDateTime arrivedAt) {
        this.id = id;
        this.visitorName = visitorName;
        this.hostStudentId = hostStudentId;
        this.arrivedAt = arrivedAt;
    }

    public VisitorEntry(String id, String visitorName, String hostStudentId,
                        LocalDateTime arrivedAt, LocalDateTime departedAt) {
        this(id, visitorName, hostStudentId, arrivedAt);
        this.departedAt = departedAt;
    }

    @Override public String id() { return id; }
    public String visitorName() { return visitorName; }
    public String hostStudentId() { return hostStudentId; }
    public LocalDateTime arrivedAt() { return arrivedAt; }
    public LocalDateTime departedAt() { return departedAt; }
    public boolean isInside() { return departedAt == null; }

    public void depart(LocalDateTime when) {
        if (!isInside()) throw new IllegalStateException("Visitor already left");
        this.departedAt = when;
    }
}
