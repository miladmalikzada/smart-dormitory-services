package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Repository;
import com.dorm.domain.Room;

import java.util.List;
import java.util.function.Consumer;

public class TicketService implements TicketOperations {
    private final Repository<MaintenanceTicket> tickets;
    private final Repository<Room> rooms;
    private final EventPublisher events;

    public TicketService(Repository<MaintenanceTicket> tickets, Repository<Room> rooms, EventPublisher events) {
        this.tickets = tickets;
        this.rooms = rooms;
        this.events = events;
    }

    @Override public MaintenanceTicket create(String roomId, String description, Priority priority) {
        rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Unknown room " + roomId));
        MaintenanceTicket t = MaintenanceTicket.builder()
                .id(Ids.next("T")).roomId(roomId).description(description).priority(priority).build();
        tickets.save(t);
        events.publish(new DormEvent("TICKET_CREATED", "Ticket " + t.id() + " (" + priority + ") for room " + roomId));
        return t;
    }

    @Override public void assign(String id, String technician) {
        change(id, t -> t.assignTo(technician), "TICKET_ASSIGNED", "Ticket " + id + " assigned to " + technician);
    }

    @Override public void start(String id) {
        change(id, MaintenanceTicket::start, "TICKET_STARTED", "Work started on ticket " + id);
    }

    @Override public void resolve(String id) {
        change(id, MaintenanceTicket::resolve, "TICKET_RESOLVED", "Ticket " + id + " is resolved");
    }

    @Override public List<MaintenanceTicket> list() { return tickets.findAll(); }

    @Override public MaintenanceTicket get(String id) {
        return tickets.findById(id).orElseThrow(() -> new NotFoundException("Unknown ticket " + id));
    }

    @Override public void delete(String id) {
        get(id);
        tickets.delete(id);
        events.publish(new DormEvent("TICKET_DELETED", "Ticket " + id + " was deleted"));
    }

    private void change(String id, Consumer<MaintenanceTicket> action, String type, String message) {
        MaintenanceTicket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Unknown ticket " + id));
        action.accept(t);
        tickets.save(t);
        events.publish(new DormEvent(type, message));
    }
}
