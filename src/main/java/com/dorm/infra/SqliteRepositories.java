package com.dorm.infra;

import com.dorm.domain.Announcement;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** The five SQLite repositories, one per table. Services only see the Repository interface. */
public final class SqliteRepositories {
    private SqliteRepositories() { }

    private static LocalDateTime time(String s) { return s == null ? null : LocalDateTime.parse(s); }

    public static final class Students extends SqlRepository<Student> {
        public Students(Connection db) { super(db); }
        @Override protected String table() { return "students"; }
        @Override protected Student map(ResultSet r) throws SQLException {
            return new Student(r.getString("id"), r.getString("name"), r.getString("phone"));
        }
        @Override protected void upsert(Student s) throws SQLException {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR REPLACE INTO students(id, name, phone) VALUES (?,?,?)")) {
                ps.setString(1, s.id()); ps.setString(2, s.name()); ps.setString(3, s.phone());
                ps.executeUpdate();
            }
        }
    }

    public static final class Rooms extends SqlRepository<Room> {
        public Rooms(Connection db) { super(db); }
        @Override protected String table() { return "rooms"; }

        @Override protected Room map(ResultSet r) throws SQLException {
            Room room = new Room(r.getString("id"), r.getInt("capacity"));
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT student_id FROM room_occupants WHERE room_id = ? ORDER BY position")) {
                ps.setString(1, room.id());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) room.assign(rs.getString(1));
                }
            }
            return room;
        }

        @Override protected void upsert(Room room) throws SQLException {
            boolean auto = db.getAutoCommit();
            db.setAutoCommit(false);
            try {
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO rooms(id, capacity) VALUES (?,?) ON CONFLICT(id) DO UPDATE SET capacity = excluded.capacity")) {
                    ps.setString(1, room.id()); ps.setInt(2, room.capacity());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = db.prepareStatement("DELETE FROM room_occupants WHERE room_id = ?")) {
                    ps.setString(1, room.id());
                    ps.executeUpdate();
                }
                int pos = 0;
                for (String student : room.occupants()) {
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO room_occupants(room_id, student_id, position) VALUES (?,?,?)")) {
                        ps.setString(1, room.id()); ps.setString(2, student); ps.setInt(3, pos++);
                        ps.executeUpdate();
                    }
                }
                db.commit();
            } catch (SQLException e) {
                db.rollback();
                throw e;
            } finally {
                db.setAutoCommit(auto);
            }
        }
    }

    public static final class Tickets extends SqlRepository<MaintenanceTicket> {
        public Tickets(Connection db) { super(db); }
        @Override protected String table() { return "tickets"; }
        @Override protected MaintenanceTicket map(ResultSet r) throws SQLException {
            return MaintenanceTicket.restore(r.getString("id"), r.getString("room_id"), r.getString("description"),
                    Priority.valueOf(r.getString("priority")), time(r.getString("created_at")),
                    r.getString("status"), r.getString("technician"));
        }
        @Override protected void upsert(MaintenanceTicket t) throws SQLException {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR REPLACE INTO tickets(id, room_id, description, priority, status, technician, created_at) VALUES (?,?,?,?,?,?,?)")) {
                ps.setString(1, t.id()); ps.setString(2, t.roomId()); ps.setString(3, t.description());
                ps.setString(4, t.priority().name()); ps.setString(5, t.status());
                ps.setString(6, t.technician()); ps.setString(7, t.createdAt().toString());
                ps.executeUpdate();
            }
        }
    }

    public static final class Visitors extends SqlRepository<VisitorEntry> {
        public Visitors(Connection db) { super(db); }
        @Override protected String table() { return "visitors"; }
        @Override protected VisitorEntry map(ResultSet r) throws SQLException {
            return new VisitorEntry(r.getString("id"), r.getString("visitor_name"), r.getString("host_student_id"),
                    time(r.getString("arrived_at")), time(r.getString("departed_at")));
        }
        @Override protected void upsert(VisitorEntry e) throws SQLException {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR REPLACE INTO visitors(id, visitor_name, host_student_id, arrived_at, departed_at) VALUES (?,?,?,?,?)")) {
                ps.setString(1, e.id()); ps.setString(2, e.visitorName()); ps.setString(3, e.hostStudentId());
                ps.setString(4, e.arrivedAt().toString());
                ps.setString(5, e.departedAt() == null ? null : e.departedAt().toString());
                ps.executeUpdate();
            }
        }
    }

    public static final class Announcements extends SqlRepository<Announcement> {
        public Announcements(Connection db) { super(db); }
        @Override protected String table() { return "announcements"; }
        @Override protected Announcement map(ResultSet r) throws SQLException {
            return new Announcement(r.getString("id"), r.getString("title"), r.getString("body"), time(r.getString("posted_at")));
        }
        @Override protected void upsert(Announcement a) throws SQLException {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR REPLACE INTO announcements(id, title, body, posted_at) VALUES (?,?,?,?)")) {
                ps.setString(1, a.id()); ps.setString(2, a.title()); ps.setString(3, a.body());
                ps.setString(4, a.postedAt().toString());
                ps.executeUpdate();
            }
        }
    }
}
