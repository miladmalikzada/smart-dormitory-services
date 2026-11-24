package com.dorm.ui;

import com.dorm.domain.Announcement;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.VisitorEntry;
import com.dorm.service.AccessDeniedException;
import com.dorm.service.AnnouncementService;
import com.dorm.service.RoomService;
import com.dorm.service.SessionContext;
import com.dorm.service.TicketOperations;
import com.dorm.service.VisitorService;

import java.io.PrintStream;
import java.util.Scanner;

/** Console UI. It only calls services; it contains no business rules. */
public class ConsoleMenu {
    private final Scanner in;
    private final PrintStream out;
    private final RoomService rooms;
    private final TicketOperations tickets;
    private final VisitorService visitors;
    private final AnnouncementService announcements;
    private final SessionContext session;

    public ConsoleMenu(Scanner in, PrintStream out, RoomService rooms, TicketOperations tickets,
                       VisitorService visitors, AnnouncementService announcements, SessionContext session) {
        this.in = in;
        this.out = out;
        this.rooms = rooms;
        this.tickets = tickets;
        this.visitors = visitors;
        this.announcements = announcements;
        this.session = session;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            if (!in.hasNextLine()) return;
            String choice = in.nextLine().trim();
            try {
                running = handle(choice);
            } catch (AccessDeniedException e) {
                out.println("Access denied: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.println("Error: " + e.getMessage());
            }
        }
        out.println("Goodbye.");
    }

    private boolean handle(String choice) {
        switch (choice) {
            case "1" -> out.println("Registered " + rooms.registerStudent(ask("Name"), ask("Phone")).id());
            case "2" -> out.println("Assigned to room " + rooms.requestRoom(ask("Student id")).id());
            case "3" -> out.println(rooms.undoLast().map(d -> "Undone: " + d).orElse("Nothing to undo"));
            case "4" -> listRooms();
            case "5" -> out.println("Ticket " + tickets.create(ask("Room id"), ask("Problem"),
                    Priority.valueOf(ask("Priority (LOW/MEDIUM/HIGH/URGENT)").toUpperCase())).id());
            case "6" -> { tickets.assign(ask("Ticket id"), ask("Technician")); out.println("Assigned"); }
            case "7" -> { tickets.start(ask("Ticket id")); out.println("Started"); }
            case "8" -> { tickets.resolve(ask("Ticket id")); out.println("Resolved"); }
            case "9" -> listTickets();
            case "10" -> out.println("Visitor entry " + visitors.arrive(ask("Visitor name"), ask("Host student id")).id());
            case "11" -> { visitors.depart(ask("Entry id")); out.println("Departure recorded"); }
            case "12" -> listVisitorsInside();
            case "13" -> { announcements.post(ask("Title"), ask("Text")); out.println("Posted"); }
            case "14" -> announcements.list().forEach(this::printAnnouncement);
            case "15" -> { session.switchTo(Role.valueOf(ask("Role (STUDENT/STAFF/ADMIN)").toUpperCase())); out.println("Role: " + session.role()); }
            case "0" -> { return false; }
            default -> out.println("Unknown option");
        }
        return true;
    }

    private void printMenu() {
        out.println();
        out.println("=== Smart Dormitory Services (role: " + session.role() + ") ===");
        out.println(" 1 Register student      2 Request room        3 Undo last room action  4 List rooms");
        out.println(" 5 Report problem        6 Assign ticket       7 Start work   8 Resolve  9 List tickets");
        out.println("10 Visitor arrives      11 Visitor leaves     12 Visitors inside");
        out.println("13 Post announcement    14 Announcements      15 Switch role     0 Exit");
        out.print("> ");
    }

    private String ask(String label) {
        out.print(label + ": ");
        return in.nextLine().trim();
    }

    private void listRooms() {
        for (Room r : rooms.rooms()) {
            out.println(r.id() + "  " + r.occupants().size() + "/" + r.capacity() + "  " + r.occupants());
        }
    }

    private void listTickets() {
        for (MaintenanceTicket t : tickets.list()) {
            out.println(t.id() + "  room " + t.roomId() + "  " + t.priority() + "  " + t.status()
                    + (t.technician() == null ? "" : "  tech=" + t.technician()) + "  - " + t.description());
        }
    }

    private void listVisitorsInside() {
        for (VisitorEntry e : visitors.log().inside()) {
            out.println(e.id() + "  " + e.visitorName() + " visiting " + e.hostStudentId() + " since " + e.arrivedAt());
        }
    }

    private void printAnnouncement(Announcement a) {
        out.println("* " + a.title() + " - " + a.body() + " (" + a.postedAt() + ")");
    }
}
