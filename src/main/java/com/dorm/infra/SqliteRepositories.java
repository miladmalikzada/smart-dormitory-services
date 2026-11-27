package com.dorm.infra;

import com.dorm.domain.Announcement;
import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.service.FieldCipher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** The five SQLite repositories, one per table. Services only see the Repository interface. */
public final class SqliteRepositories {
    private SqliteRepositories() { }

    private static LocalDateTime time(String s) { return s == null ? null : LocalDateTime.parse(s); }

    /** Identity numbers (IIN, national ID, passport) are encrypted before they reach the database file. */
    public static final class Students extends SqlRepository<Student> {
        private final FieldCipher cipher;
        public Students(Connection db, FieldCipher cipher) { super(db); this.cipher = cipher; }
        @Override protected String table() { return "students"; }
        @Override protected Student map(ResultSet r) throws SQLException {
            String dob = r.getString("date_of_birth");
            int year = r.getInt("course_year");
            return new Student(r.getString("id"), r.getString("first_name"), r.getString("last_name"),
                    dob == null ? null : LocalDate.parse(dob), r.getString("gender"), r.getString("nationality"),
                    cipher.decrypt(r.getString("iin")), cipher.decrypt(r.getString("national_id_number")),
                    cipher.decrypt(r.getString("passport_number")), r.getString("phone"), r.getString("email"),
                    r.getString("faculty"), r.wasNull() ? null : year, r.getString("address"),
                    r.getString("emergency_contact_name"), r.getString("emergency_contact_phone"));
        }
        @Override protected void upsert(Student s) throws SQLException {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR REPLACE INTO students(id, first_name, last_name, date_of_birth, gender, nationality, iin, "
                    + "national_id_number, passport_number, phone, email, faculty, course_year, address, "
                    + "emergency_contact_name, emergency_contact_phone) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, s.id()); ps.setString(2, s.firstName()); ps.setString(3, s.lastName());
                ps.setString(4, s.dateOfBirth() == null ? null : s.dateOfBirth().toString());
                ps.setString(5, s.gender()); ps.setString(6, s.nationality());
                ps.setString(7, cipher.encrypt(s.iin()));
                ps.setString(8, cipher.encrypt(s.nationalId()));
                ps.setString(9, cipher.encrypt(s.passportNumber()));
                ps.setString(10, s.phone()); ps.setString(11, s.email()); ps.setString(12, s.faculty());
                if (s.courseYear() == null) ps.setNull(13, java.sql.Types.INTEGER); else ps.setInt(13, s.courseYear());
                ps.setString(14, s.address()); ps.setString(15, s.emergencyName()); ps.setString(16, s.emergencyPhone());
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
