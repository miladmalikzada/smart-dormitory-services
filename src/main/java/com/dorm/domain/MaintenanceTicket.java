package com.dorm.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MaintenanceTicket implements Entity, Serializable {
    private final String id;
    private final String roomId;
    private final String description;
    private final Priority priority;
    private final LocalDateTime createdAt;
    private TicketState state = new TicketStates.Open();
    private String technician;

    private MaintenanceTicket(Builder b) {
        this.id = b.id;
        this.roomId = b.roomId;
        this.description = b.description;
        this.priority = b.priority;
        this.createdAt = b.createdAt;
    }

    @Override public String id() { return id; }
    public String roomId() { return roomId; }
    public String description() { return description; }
    public Priority priority() { return priority; }
    public LocalDateTime createdAt() { return createdAt; }
    public String status() { return state.name(); }
    public String technician() { return technician; }

    public void assignTo(String technician) {
        if (technician == null || technician.isBlank()) throw new IllegalArgumentException("technician required");
        state = state.assign();
        this.technician = technician;
    }
    public void start() { state = state.start(); }
    public void resolve() { state = state.resolve(); }

    public static Builder builder() { return new Builder(); }

    /** Builder (L9): validates required fields and gives defaults for optional ones. */
    public static final class Builder {
        private String id;
        private String roomId;
        private String description;
        private Priority priority = Priority.MEDIUM;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder roomId(String roomId) { this.roomId = roomId; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder priority(Priority p) { this.priority = p; return this; }
        public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }

        public MaintenanceTicket build() {
            if (id == null || roomId == null) throw new IllegalStateException("id and roomId are required");
            if (description == null || description.isBlank()) throw new IllegalStateException("description is required");
            return new MaintenanceTicket(this);
        }
    }
}
