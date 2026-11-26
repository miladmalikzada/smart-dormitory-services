package com.dorm.service;

import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;

import java.util.List;

/** Subject interface shared by the real service and its access-control Proxy. */
public interface TicketOperations {
    MaintenanceTicket create(String roomId, String description, Priority priority);
    void assign(String ticketId, String technician);
    void start(String ticketId);
    void resolve(String ticketId);
    List<MaintenanceTicket> list();
    MaintenanceTicket get(String ticketId);
    void delete(String ticketId);
}
