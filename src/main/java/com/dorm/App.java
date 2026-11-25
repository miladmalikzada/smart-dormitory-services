package com.dorm;

import com.dorm.domain.Announcement;
import com.dorm.domain.Entity;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Repository;
import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.infra.ConsoleChannelFactory;
import com.dorm.infra.FileRepository;
import com.dorm.infra.InMemoryRepository;
import com.dorm.infra.SmsChannelFactory;
import com.dorm.infra.SqliteDatabase;
import com.dorm.infra.SqliteRepositories;
import com.dorm.service.AnnouncementService;
import com.dorm.service.AuditLogListener;
import com.dorm.service.CommandHistory;
import com.dorm.service.EventPublisher;
import com.dorm.service.LeastOccupiedStrategy;
import com.dorm.service.NotificationChannel;
import com.dorm.service.NotificationListener;
import com.dorm.service.RoomService;
import com.dorm.service.SecuredTicketService;
import com.dorm.service.SessionContext;
import com.dorm.service.TicketOperations;
import com.dorm.service.TicketService;
import com.dorm.service.VisitorService;
import com.dorm.ui.ConsoleMenu;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Composition root (L14): the ONLY place where concrete classes are chosen and wired.
 * Usage: App [--storage=memory|file|sqlite] [--data=data]
 */
public final class App {
    private App() { }

    public static void main(String[] args) {
        String storage = "memory";
        Path dataDir = Path.of("data");
        for (String a : args) {
            if (a.startsWith("--storage=")) storage = a.substring("--storage=".length());
            if (a.startsWith("--data=")) dataDir = Path.of(a.substring("--data=".length()));
        }
        build(storage, dataDir, System.in, System.out).run();
    }

    public static ConsoleMenu build(String storage, Path dataDir, InputStream input, PrintStream out) {
        Clock clock = Clock.systemDefaultZone();

        Repository<Student> studentRepo;
        Repository<Room> roomRepo;
        Repository<MaintenanceTicket> ticketRepo;
        Repository<VisitorEntry> visitorRepo;
        Repository<Announcement> announcementRepo;
        if ("sqlite".equals(storage)) {
            Connection db = SqliteDatabase.open(dataDir.resolve("dorm.db"));
            studentRepo = new SqliteRepositories.Students(db);
            roomRepo = new SqliteRepositories.Rooms(db);
            ticketRepo = new SqliteRepositories.Tickets(db);
            visitorRepo = new SqliteRepositories.Visitors(db);
            announcementRepo = new SqliteRepositories.Announcements(db);
        } else {
            studentRepo = repo(storage, dataDir, "students");
            roomRepo = repo(storage, dataDir, "rooms");
            ticketRepo = repo(storage, dataDir, "tickets");
            visitorRepo = repo(storage, dataDir, "visitors");
            announcementRepo = repo(storage, dataDir, "announcements");
        }

        // Notification channels: add a new channel = new ChannelFactory subclass + one line here.
        List<NotificationChannel> channels = List.of(
                new ConsoleChannelFactory(clock, out).create(),
                new SmsChannelFactory(clock, out).create());

        EventPublisher events = new EventPublisher();
        events.subscribe(new NotificationListener(channels, Set.of("ANNOUNCEMENT_POSTED", "TICKET_RESOLVED")));
        events.subscribe(new AuditLogListener());

        SessionContext session = new SessionContext(Role.STUDENT);
        RoomService roomService = new RoomService(studentRepo, roomRepo, new LeastOccupiedStrategy(),
                new CommandHistory(), events);
        TicketOperations tickets = new SecuredTicketService(new TicketService(ticketRepo, roomRepo, events), session);
        VisitorService visitors = new VisitorService(visitorRepo, studentRepo, events, clock);
        AnnouncementService announcements = new AnnouncementService(announcementRepo, events, clock);

        if (roomService.rooms().isEmpty()) {
            roomService.addRoom("R101", 2);
            roomService.addRoom("R102", 2);
            roomService.addRoom("R201", 3);
        }
        return new ConsoleMenu(new Scanner(input), out, roomService, tickets, visitors, announcements, session);
    }

    private static <T extends Entity & Serializable> Repository<T> repo(String storage, Path dir, String name) {
        return switch (storage) {
            case "memory" -> new InMemoryRepository<>();
            case "file" -> new FileRepository<>(dir.resolve(name + ".bin"));
            default -> throw new IllegalArgumentException("Unknown storage: " + storage);
        };
    }
}
