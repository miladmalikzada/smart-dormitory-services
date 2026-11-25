package com.dorm.domain;

/** Concrete states of a maintenance ticket: OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED. */
public final class TicketStates {
    private TicketStates() { }

    public static final class Open implements TicketState {
        public String name() { return "OPEN"; }
        @Override public TicketState assign() { return new Assigned(); }
    }

    public static final class Assigned implements TicketState {
        public String name() { return "ASSIGNED"; }
        @Override public TicketState start() { return new InProgress(); }
    }

    public static final class InProgress implements TicketState {
        public String name() { return "IN_PROGRESS"; }
        @Override public TicketState resolve() { return new Resolved(); }
    }

    public static final class Resolved implements TicketState {
        public String name() { return "RESOLVED"; }
    }

    /** Rebuilds a state from its stored name (used when loading from a database). */
    public static TicketState fromName(String name) {
        return switch (name) {
            case "OPEN" -> new Open();
            case "ASSIGNED" -> new Assigned();
            case "IN_PROGRESS" -> new InProgress();
            case "RESOLVED" -> new Resolved();
            default -> throw new IllegalArgumentException("Unknown ticket status " + name);
        };
    }
}
