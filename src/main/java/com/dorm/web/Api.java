package com.dorm.web;

import com.dorm.domain.Announcement;
import com.dorm.domain.DormEvent;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.service.AccessDeniedException;
import com.dorm.service.AnnouncementService;
import com.dorm.service.AuditLogListener;
import com.dorm.service.NotFoundException;
import com.dorm.service.RoomAdminService;
import com.dorm.service.RoomService;
import com.dorm.service.SessionContext;
import com.dorm.service.StudentService;
import com.dorm.service.TicketOperations;
import com.dorm.service.VisitorService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API logic (no sockets here, so it can be unit-tested). It only calls services.
 * Resources: students, rooms, room-requests, tickets, visitors, announcements, summary, events.
 */
public class Api {
    public record Response(int status, Object body) { }

    private static class ApiException extends RuntimeException {
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }

    private final StudentService students;
    private final RoomAdminService roomAdmin;
    private final RoomService rooms;
    private final TicketOperations tickets;
    private final VisitorService visitors;
    private final AnnouncementService announcements;
    private final SessionContext session;
    private final AuditLogListener audit;

    public Api(StudentService students, RoomAdminService roomAdmin, RoomService rooms, TicketOperations tickets,
               VisitorService visitors, AnnouncementService announcements, SessionContext session, AuditLogListener audit) {
        this.students = students;
        this.roomAdmin = roomAdmin;
        this.rooms = rooms;
        this.tickets = tickets;
        this.visitors = visitors;
        this.announcements = announcements;
        this.session = session;
        this.audit = audit;
    }

    /** Handles one request. Requests are processed one at a time (see WebServer), so switching the role is safe. */
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
            case "POST" -> created(json(students.register(str(b, "name"), str(b, "phone"))));
            case "PUT" -> ok(json(students.update(need(id), str(b, "name"), str(b, "phone"))));
            case "DELETE" -> { students.delete(need(id)); yield deleted(id); }
            default -> notAllowed();
        };
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
        m.put("id", s.id()); m.put("name", s.name()); m.put("phone", s.phone());
        return m;
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
