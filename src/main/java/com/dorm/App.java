package com.dorm;

import com.dorm.domain.Announcement;
import com.dorm.domain.Entity;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Repository;
import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.infra.AesGcmCipher;
import com.dorm.infra.ConsoleChannelFactory;
import com.dorm.infra.EnvConfig;
import com.dorm.infra.FileRepository;
import com.dorm.infra.InMemoryRepository;
import com.dorm.infra.SmsChannelFactory;
import com.dorm.infra.SqliteAccountStore;
import com.dorm.infra.SqliteDatabase;
import com.dorm.infra.SqliteRepositories;
import com.dorm.service.AccountStore;
import com.dorm.service.AnnouncementService;
import com.dorm.service.MemoryAccountStore;
import com.dorm.service.AuditLogListener;
import com.dorm.service.CertificateOperations;
import com.dorm.service.CertificateService;
import com.dorm.service.CsvCertificateFormat;
import com.dorm.service.HtmlCertificateFormat;
import com.dorm.service.SecuredCertificateService;
import com.dorm.service.TextCertificateFormat;
import com.dorm.service.AuthService;
import com.dorm.service.SecuredStudentService;
import com.dorm.service.StudentOperations;
import com.dorm.service.CommandHistory;
import com.dorm.service.EventPublisher;
import com.dorm.service.LeastOccupiedStrategy;
import com.dorm.service.NotificationChannel;
import com.dorm.service.NotificationListener;
import com.dorm.service.RoomAdminService;
import com.dorm.service.RoomService;
import com.dorm.service.SecuredTicketService;
import com.dorm.service.SessionContext;
import com.dorm.service.StudentService;
import com.dorm.service.TicketOperations;
import com.dorm.service.TicketService;
import com.dorm.service.VisitorService;
import com.dorm.ui.ConsoleMenu;
import com.dorm.web.Api;
import com.dorm.web.WebServer;

import java.io.IOException;
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
 * Console: App [--storage=memory|file|sqlite] [--data=data]
 * Website: App --web [--port=8080] [--storage=sqlite] [--data=data]
 */
public final class App {
    private App() { }

    /** Everything the user interfaces need, wired once. */
    public record Services(RoomService rooms, StudentOperations students, RoomAdminService roomAdmin,
                           TicketOperations tickets, VisitorService visitors, AnnouncementService announcements,
                           SessionContext session, AuditLogListener audit, Api api) { }

    public static void main(String[] args) throws IOException, InterruptedException {
        String storage = "memory";
        Path dataDir = Path.of("data");
        boolean web = false;
        int port = 8080;
        for (String a : args) {
            if (a.startsWith("--storage=")) storage = a.substring("--storage=".length());
            if (a.startsWith("--data=")) dataDir = Path.of(a.substring("--data=".length()));
            if (a.startsWith("--port=")) port = Integer.parseInt(a.substring("--port=".length()));
            if (a.equals("--web")) web = true;
        }
        EnvConfig cfg = EnvConfig.load(Path.of(".env"));
        if (web) {
            WebServer server = buildWeb(storage, dataDir, port, System.out, cfg);
            server.start();
            System.out.println("Smart Dormitory Services is running (storage: " + storage + ")");
            System.out.println("Open  http://localhost:" + server.port() + "   (press Ctrl+C to stop)");
            Thread.currentThread().join();
        } else {
            build(storage, dataDir, System.in, System.out, cfg).run();
        }
    }

    public static ConsoleMenu build(String storage, Path dataDir, InputStream input, PrintStream out, EnvConfig cfg) {
        Services s = wire(storage, dataDir, out, cfg);
        return new ConsoleMenu(new Scanner(input), out, s.rooms(), s.tickets(), s.visitors(), s.announcements(), s.session());
    }

    /** The website needs the three login passwords from .env; it refuses to start without them. */
    public static WebServer buildWeb(String storage, Path dataDir, int port, PrintStream out, EnvConfig cfg) throws IOException {
        cfg.require("DORM_ADMIN_PASSWORD");
        return new WebServer(wire(storage, dataDir, out, cfg).api(), port);
    }

    public static Services wire(String storage, Path dataDir, PrintStream out, EnvConfig cfg) {
        Clock clock = Clock.systemDefaultZone();

        Repository<Student> studentRepo;
        Repository<Room> roomRepo;
        Repository<MaintenanceTicket> ticketRepo;
        Repository<VisitorEntry> visitorRepo;
        Repository<Announcement> announcementRepo;
        AccountStore accountStore;
        if ("sqlite".equals(storage)) {
            Connection db = SqliteDatabase.open(dataDir.resolve("dorm.db"));
            studentRepo = new SqliteRepositories.Students(db, AesGcmCipher.fromBase64(cfg.require("DORM_ENCRYPTION_KEY")));
            roomRepo = new SqliteRepositories.Rooms(db);
            ticketRepo = new SqliteRepositories.Tickets(db);
            visitorRepo = new SqliteRepositories.Visitors(db);
            announcementRepo = new SqliteRepositories.Announcements(db);
            accountStore = new SqliteAccountStore(db);
        } else {
            studentRepo = repo(storage, dataDir, "students");
            roomRepo = repo(storage, dataDir, "rooms");
            ticketRepo = repo(storage, dataDir, "tickets");
            visitorRepo = repo(storage, dataDir, "visitors");
            announcementRepo = repo(storage, dataDir, "announcements");
            accountStore = new MemoryAccountStore();   // accounts survive restarts only with --storage=sqlite
        }

        // Notification channels: add a new channel = new ChannelFactory subclass + one line here.
        List<NotificationChannel> channels = List.of(
                new ConsoleChannelFactory(clock, out).create(),
                new SmsChannelFactory(clock, out).create());

        EventPublisher events = new EventPublisher();
        events.subscribe(new NotificationListener(channels, Set.of("ANNOUNCEMENT_POSTED", "TICKET_RESOLVED")));
        AuditLogListener audit = new AuditLogListener();
        events.subscribe(audit);

        SessionContext session = new SessionContext(Role.STUDENT);
        RoomService roomService = new RoomService(studentRepo, roomRepo, new LeastOccupiedStrategy(),
                new CommandHistory(), events);
        StudentOperations studentService = new SecuredStudentService(
                new StudentService(studentRepo, roomRepo, visitorRepo), session);
        // Certificate formats: add a new format = new CertificateFormat class + one line here.
        CertificateOperations certificates = new SecuredCertificateService(new CertificateService(studentRepo, roomRepo,
                List.of(new TextCertificateFormat(), new HtmlCertificateFormat(), new CsvCertificateFormat()), clock), session);
        AuthService auth = new AuthService(clock, accountStore);
        // The owner's account always comes from .env, so nobody can lock the owner out.
        String adminPassword = cfg.get("DORM_ADMIN_PASSWORD");
        if (adminPassword != null) auth.addAccount("admin", adminPassword, Role.ADMIN);
        RoomAdminService roomAdmin = new RoomAdminService(roomRepo, ticketRepo);
        TicketOperations tickets = new SecuredTicketService(new TicketService(ticketRepo, roomRepo, events), session);
        VisitorService visitors = new VisitorService(visitorRepo, studentRepo, events, clock);
        AnnouncementService announcements = new AnnouncementService(announcementRepo, events, clock);

        if (roomService.rooms().isEmpty()) {
            roomService.addRoom("R101", 2);
            roomService.addRoom("R102", 2);
            roomService.addRoom("R201", 3);
        }
        Api api = new Api(auth, certificates, studentService, roomAdmin, roomService, tickets, visitors, announcements, session, audit);
        return new Services(roomService, studentService, roomAdmin, tickets, visitors, announcements, session, audit, api);
    }

    private static <T extends Entity & Serializable> Repository<T> repo(String storage, Path dir, String name) {
        return switch (storage) {
            case "memory" -> new InMemoryRepository<>();
            case "file" -> new FileRepository<>(dir.resolve(name + ".bin"));
            default -> throw new IllegalArgumentException("Unknown storage: " + storage);
        };
    }
}
