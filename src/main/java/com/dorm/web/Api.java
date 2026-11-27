package com.dorm.web;

import com.dorm.domain.AccountStatus;
import com.dorm.domain.Announcement;
import com.dorm.domain.DormEvent;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.service.AccessDeniedException;
import com.dorm.service.AccountView;
import com.dorm.service.AnnouncementService;
import com.dorm.service.AuditLogListener;
import com.dorm.service.AuthService;
import com.dorm.service.AuthenticationException;
import com.dorm.service.CertificateOperations;
import com.dorm.service.IssuedDocument;
import com.dorm.service.StudentExporter;
import com.dorm.service.RateLimitedException;
import com.dorm.service.StudentData;
import com.dorm.service.StudentOperations;
import com.dorm.service.NotFoundException;
import com.dorm.service.RoomAdminOperations;
import com.dorm.service.RoomService;
import com.dorm.service.SessionContext;
import com.dorm.service.TicketOperations;
import com.dorm.service.VisitorService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API logic (no sockets here, so it can be unit-tested). It only calls services.
 * Public endpoint: POST /api/login. Everything else needs "Authorization: Bearer <token>".
 * Resources: students, rooms, room-requests, tickets, visitors, announcements, summary, events.
 */
public class Api {
    public record Response(int status, Object body) { }

    private static class ApiException extends RuntimeException {
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }

    private final StudentOperations students;
    private final AuthService auth;
    private final CertificateOperations certificates;
    private final RoomAdminOperations roomAdmin;
    private final RoomService rooms;
    private final TicketOperations tickets;
    private final VisitorService visitors;
    private final AnnouncementService announcements;
    private final SessionContext session;
    private final AuditLogListener audit;

    public Api(AuthService auth, CertificateOperations certificates, StudentOperations students, RoomAdminOperations roomAdmin, RoomService rooms, TicketOperations tickets,
               VisitorService visitors, AnnouncementService announcements, SessionContext session, AuditLogListener audit) {
        this.auth = auth;
        this.certificates = certificates;
        this.students = students;
        this.roomAdmin = roomAdmin;
        this.rooms = rooms;
        this.tickets = tickets;
        this.visitors = visitors;
        this.announcements = announcements;
        this.session = session;
        this.audit = audit;
    }

    /** Entry point for the web server: logs in, or checks the Bearer token, then routes the request. */
    public Response handleRequest(String method, String path, String bodyText, String authorization) {
        String clean = path.replaceAll("^/+|/+$", "");
        try {
            if (clean.equals("api/login")) {
                if (!method.equals("POST")) return notAllowed();
                Map<String, Object> b = Json.parseObject(bodyText);
                AuthService.Login l = auth.login(str(b, "username"), str(b, "password"));
                return ok(loginJson(l));
            }
            if (clean.equals("api/signup")) {
                if (!method.equals("POST")) return notAllowed();
                Map<String, Object> b = Json.parseObject(bodyText);
                AccountView a = auth.signUp(str(b, "username"), str(b, "password"), accountType(b));
                Map<String, Object> out = accountJson(a);
                out.put("message", a.status() == AccountStatus.ACTIVE
                        ? "Account created. You can sign in now."
                        : "Request sent. You can sign in after the administrator approves it.");
                return created(out);
            }
            String token = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                    ? authorization.substring(7).trim() : null;
            AuthService.Login who = auth.authenticate(token)
                    .orElseThrow(() -> new AuthenticationException("Please sign in"));
            if (clean.equals("api/logout")) {
                auth.logout(token);
                return ok(Map.of("loggedOut", true));
            }
            if (clean.equals("api/me")) {
                Map<String, Object> me = loginJson(who);
                me.remove("token");
                return ok(me);
            }
            if (clean.equals("api/accounts") || clean.startsWith("api/accounts/")) return accounts(method, clean, who);
            return handle(method, path, bodyText, who.role());
        } catch (AuthenticationException e) {
            return error(401, e.getMessage());
        } catch (RateLimitedException e) {
            return error(429, e.getMessage());
        } catch (NotFoundException e) {
            return error(404, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return error(409, e.getMessage());
        }
    }

    /** Administrator only: GET /api/accounts, POST /api/accounts/{username}/approve or /reject. */
    private Response accounts(String method, String clean, AuthService.Login who) {
        if (who.role() != Role.ADMIN) return error(403, "Only an administrator can manage accounts");
        String[] p = clean.split("/");
        if (p.length == 2) return method.equals("GET") ? ok(auth.listAccounts().stream().map(Api::accountJson).toList()) : notAllowed();
        if (p.length != 4 || !method.equals("POST")) return method.equals("POST") ? error(404, "Unknown endpoint /" + clean) : notAllowed();
        return switch (p[3]) {
            case "approve" -> ok(accountJson(auth.approve(who.username(), p[2])));
            case "reject" -> ok(accountJson(auth.reject(who.username(), p[2])));
            default -> error(404, "Unknown endpoint /" + clean);
        };
    }

    private static Role accountType(Map<String, Object> b) {
        String type = str(b, "role");
        if (type == null) return null;
        try {
            return Role.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Choose an account type: student, staff or admin");
        }
    }

    private static Map<String, Object> accountJson(AccountView a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", a.username());
        m.put("role", a.role().name());
        m.put("status", a.status().name());
        m.put("createdAt", a.createdAt().toString());
        return m;
    }

    private static Map<String, Object> loginJson(AuthService.Login l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", l.username());
        m.put("role", l.role().name());
        m.put("expiresAt", l.expiresAt().toString());
        if (l.token() != null) m.put("token", l.token());
        return m;
    }

    /** Handles one request for a known role. Requests are processed one at a time (see WebServer), so switching the role is safe. */
    public Response handle(String method, String path, String bodyText, Role role) {
        try {
            session.switchTo(role);
            Map<String, Object> body = Json.parseObject(bodyText);
            String[] p = path.replaceAll("^/+|/+$", "").split("/");
            if (p.length < 2 || !p[0].equals("api")) throw new NotFoundException("Unknown endpoint " + path);
            String id = p.length > 2 ? p[2] : null;
            return switch (p[1]) {
                case "students" -> students(method, id, body);
                case "rooms" -> rooms(method, id, p.length > 4 && p[3].equals("occupants") ? p[4] : null, body);
                case "room-requests" -> roomRequests(method, id, body);
                case "tickets" -> tickets(method, id, body);
                case "visitors" -> visitors(method, id, body);
                case "announcements" -> announcements(method, id, body);
                case "certificates" -> certificate(method, id, p.length > 3 ? p[3] : null);
                case "export" -> export(method, id);
                case "summary" -> ok(summary());
                case "events" -> ok(events());
                default -> throw new NotFoundException("Unknown endpoint " + path);
            };
        } catch (ApiException e) {
            return error(e.status, e.getMessage());
        } catch (AccessDeniedException e) {
            return error(403, e.getMessage());
        } catch (NotFoundException e) {
            return error(404, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return error(409, e.getMessage());
        } catch (RuntimeException e) {
            return error(500, "Unexpected error: " + e.getClass().getSimpleName());
        }
    }

    // ---- resources -------------------------------------------------------------------------------

    private Response students(String m, String id, Map<String, Object> b) {
        return switch (m) {
            case "GET" -> ok(id == null ? students.list().stream().map(this::json).toList() : json(students.get(id)));
            case "POST" -> created(registerStudent(b));
            case "PUT" -> ok(json(students.update(need(id), studentData(b))));
            case "DELETE" -> { students.delete(need(id)); yield deleted(id); }
            default -> notAllowed();
        };
    }

    /** GET /api/certificates/{studentId}/{text|html|csv} */
    /** Registers a student; with "assignRoom" set, also places them in the room the allocation strategy picks. */
    private Map<String, Object> registerStudent(Map<String, Object> b) {
        Student s = students.register(studentData(b));
        Map<String, Object> out = new LinkedHashMap<>(json(s));
        if (flag(b, "assignRoom")) {
            try {
                out.put("roomId", rooms.requestRoom(s.id()).id());
            } catch (IllegalStateException e) {
                out.put("roomMessage", "No free room is available. Add a room or free a bed, then use Request room.");
            }
        }
        return out;
    }

    private static boolean flag(Map<String, Object> b, String key) {
        Object v = b.get(key);
        return v instanceof Boolean x ? x : v != null && Set.of("true", "on", "yes").contains(String.valueOf(v).toLowerCase());
    }

    private Response certificate(String m, String studentId, String format) {
        if (!m.equals("GET")) return notAllowed();
        return ok(document(certificates.issue(need(studentId), format == null ? "text" : format)));
    }

    /** GET /api/export/students : CSV of what the caller's role may see. Staff and admin only. */
    private Response export(String m, String what) {
        if (!m.equals("GET")) return notAllowed();
        if (!"students".equals(what)) throw new NotFoundException("Unknown export " + what);
        if (session.role() == Role.STUDENT) throw new AccessDeniedException("Only STAFF or ADMIN may export students");
        return ok(document(new IssuedDocument("students.csv", "text/csv; charset=utf-8",
                StudentExporter.toCsv(students.list()))));
    }

    private static Map<String, Object> document(IssuedDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("filename", d.filename()); m.put("contentType", d.contentType()); m.put("content", d.content());
        return m;
    }

    private Response rooms(String m, String id, String occupantId, Map<String, Object> b) {
        if (occupantId != null) {
            if (!m.equals("DELETE")) return notAllowed();
            if (!roomAdmin.get(id).houses(occupantId)) throw new ApiException(409, occupantId + " does not live in " + id);
            rooms.checkOut(occupantId);
            return ok(json(roomAdmin.get(id)));
        }
        return switch (m) {
            case "GET" -> ok(id == null ? roomAdmin.list().stream().map(this::json).toList() : json(roomAdmin.get(id)));
            case "POST" -> created(json(roomAdmin.create(str(b, "id"), integer(b, "capacity"))));
            case "PUT" -> ok(json(roomAdmin.updateCapacity(need(id), integer(b, "capacity"))));
            case "DELETE" -> { roomAdmin.delete(need(id)); yield deleted(id); }
            default -> notAllowed();
        };
    }

    private Response roomRequests(String m, String id, Map<String, Object> b) {
        if (!m.equals("POST")) return notAllowed();
        if ("undo".equals(id)) {
            String what = rooms.undoLast().orElseThrow(() -> new IllegalStateException("Nothing to undo"));
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("undone", what);
            return ok(r);
        }
        return created(json(rooms.requestRoom(str(b, "studentId"))));
    }

    private Response tickets(String m, String id, Map<String, Object> b) {
        return switch (m) {
            case "GET" -> ok(id == null ? tickets.list().stream().map(this::json).toList() : json(tickets.get(id)));
            case "POST" -> created(json(tickets.create(str(b, "roomId"), str(b, "description"), priority(b))));
            case "PUT" -> {
                String action = str(b, "action");
                if (action == null) throw new IllegalArgumentException("action required (assign, start, resolve)");
                switch (action) {
                    case "assign" -> tickets.assign(need(id), str(b, "technician"));
                    case "start" -> tickets.start(need(id));
                    case "resolve" -> tickets.resolve(need(id));
                    default -> throw new IllegalArgumentException("unknown action " + action);
                }
                yield ok(json(tickets.get(id)));
            }
            case "DELETE" -> { tickets.delete(need(id)); yield deleted(id); }
            default -> notAllowed();
        };
    }

    private Response visitors(String m, String id, Map<String, Object> b) {
        return switch (m) {
            case "GET" -> {
                if (id == null) yield ok(toList(visitors.log()));
                if (id.equals("inside")) yield ok(toList(visitors.log().inside()));
                yield ok(json(visitors.get(id)));
            }
            case "POST" -> created(json(visitors.arrive(str(b, "visitorName"), str(b, "hostStudentId"))));
            case "PUT" -> { visitors.depart(need(id)); yield ok(json(visitors.get(id))); }
            case "DELETE" -> { visitors.delete(need(id)); yield deleted(id); }
            default -> notAllowed();
        };
    }

    private Response announcements(String m, String id, Map<String, Object> b) {
        return switch (m) {
            case "GET" -> ok(id == null ? announcements.list().stream().map(this::json).toList() : json(announcements.get(id)));
            case "POST" -> created(json(announcements.post(str(b, "title"), str(b, "body"))));
            case "PUT" -> ok(json(announcements.update(need(id), str(b, "title"), str(b, "body"))));
            case "DELETE" -> { announcements.delete(need(id)); yield deleted(id); }
            default -> notAllowed();
        };
    }

    private Map<String, Object> summary() {
        Map<String, Object> s = new LinkedHashMap<>();
        List<Room> all = roomAdmin.list();
        s.put("students", students.list().size());
        s.put("rooms", all.size());
        s.put("freeBeds", all.stream().mapToInt(Room::freeBeds).sum());
        s.put("openTickets", tickets.list().stream().filter(t -> !t.status().equals("RESOLVED")).count());
        s.put("visitorsInside", toList(visitors.log().inside()).size());
        s.put("announcements", announcements.list().size());
        return s;
    }

    private List<Object> events() {
        List<DormEvent> all = audit.events();
        List<Object> out = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && out.size() < 30; i--) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("type", all.get(i).type());
            e.put("message", all.get(i).message());
            out.add(e);
        }
        return out;
    }

    // ---- JSON views of domain objects ------------------------------------------------------------

    private Map<String, Object> json(Student s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id()); m.put("name", s.name());
        m.put("firstName", s.firstName()); m.put("lastName", s.lastName());
        m.put("dateOfBirth", s.dateOfBirth() == null ? null : s.dateOfBirth().toString());
        m.put("gender", s.gender()); m.put("nationality", s.nationality());
        m.put("iin", s.iin()); m.put("nationalId", s.nationalId()); m.put("passportNumber", s.passportNumber());
        m.put("phone", s.phone()); m.put("email", s.email()); m.put("faculty", s.faculty());
        m.put("courseYear", s.courseYear()); m.put("address", s.address());
        m.put("emergencyName", s.emergencyName()); m.put("emergencyPhone", s.emergencyPhone());
        return m;
    }

    private static StudentData studentData(Map<String, Object> b) {
        Integer year = null;
        Object y = b.get("courseYear");
        if (y instanceof Number n) year = n.intValue();
        else if (y != null && !String.valueOf(y).isBlank()) {
            try { year = Integer.parseInt(String.valueOf(y).trim()); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("course year must be a number"); }
        }
        return new StudentData(str(b, "firstName"), str(b, "lastName"), str(b, "dateOfBirth"), str(b, "gender"),
                str(b, "nationality"), str(b, "iin"), str(b, "nationalId"), str(b, "passportNumber"),
                str(b, "phone"), str(b, "email"), str(b, "faculty"), year, str(b, "address"),
                str(b, "emergencyName"), str(b, "emergencyPhone"));
    }

    private Map<String, Object> json(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id()); m.put("capacity", r.capacity()); m.put("freeBeds", r.freeBeds());
        m.put("occupants", r.occupants());
        return m;
    }

    private Map<String, Object> json(MaintenanceTicket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.id()); m.put("roomId", t.roomId()); m.put("description", t.description());
        m.put("priority", t.priority().name()); m.put("status", t.status());
        m.put("technician", t.technician()); m.put("createdAt", t.createdAt().toString());
        return m;
    }

    private Map<String, Object> json(VisitorEntry v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.id()); m.put("visitorName", v.visitorName()); m.put("hostStudentId", v.hostStudentId());
        m.put("arrivedAt", v.arrivedAt().toString());
        m.put("departedAt", v.departedAt() == null ? null : v.departedAt().toString());
        m.put("inside", v.isInside());
        return m;
    }

    private Map<String, Object> json(Announcement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id()); m.put("title", a.title()); m.put("body", a.body()); m.put("postedAt", a.postedAt().toString());
        return m;
    }

    private List<Object> toList(Iterable<VisitorEntry> entries) {
        List<Object> out = new ArrayList<>();
        for (VisitorEntry e : entries) out.add(json(e));
        return out;
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static String str(Map<String, Object> b, String key) {
        Object v = b.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static int integer(Map<String, Object> b, String key) {
        Object v = b.get(key);
        if (v instanceof Number n) return n.intValue();
        throw new IllegalArgumentException(key + " must be a number");
    }

    private static Priority priority(Map<String, Object> b) {
        String p = str(b, "priority");
        if (p == null) return Priority.MEDIUM;
        try { return Priority.valueOf(p.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("priority must be LOW, MEDIUM, HIGH or URGENT"); }
    }

    private static String need(String id) {
        if (id == null) throw new ApiException(400, "id required in the URL");
        return id;
    }

    private static Response ok(Object body) { return new Response(200, body); }
    private static Response created(Object body) { return new Response(201, body); }
    private static Response notAllowed() { return new Response(405, Map.of("error", "Method not allowed")); }
    private static Response error(int status, String msg) { return new Response(status, Map.of("error", String.valueOf(msg))); }

    private static Response deleted(String id) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("deleted", id);
        return ok(m);
    }
}
