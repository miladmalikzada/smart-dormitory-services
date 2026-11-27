package com.dorm.service;

import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Role;

import java.util.List;

/**
 * Protection Proxy (L11): same interface as TicketService, but only staff may change a ticket.
 * The real service contains no security code.
 */
public class SecuredTicketService implements TicketOperations {
    private final TicketOperations target;
    private final SessionContext session;

    public SecuredTicketService(TicketOperations target, SessionContext session) {
        this.target = target;
        this.session = session;
    }

    @Override public MaintenanceTicket create(String roomId, String description, Priority priority) {
        return target.create(roomId, description, priority);
    }

    @Override public void assign(String ticketId, String technician) { requireStaff(); target.assign(ticketId, technician); }
    @Override public void start(String ticketId) { requireStaff(); target.start(ticketId); }
    @Override public void resolve(String ticketId) { requireStaff(); target.resolve(ticketId); }
    @Override public List<MaintenanceTicket> list() { return target.list(); }

    @Override public MaintenanceTicket get(String ticketId) { return target.get(ticketId); }
    @Override public void delete(String ticketId) { requireStaff(); target.delete(ticketId); }

    private void requireStaff() {
        if (session.role() == Role.STUDENT) {
            throw new AccessDeniedException("Only STAFF or ADMIN may change tickets");
        }
    }
}
