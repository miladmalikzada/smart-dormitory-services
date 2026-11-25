package com.dorm.infra;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.MaintenanceTicket;
import com.dorm.domain.Priority;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Uses a temporary SQLite file: no server and no network, so it is allowed by the course rules. */
class SqliteRepositoryTest {
    @TempDir Path tmp;
    private Connection db;

    @BeforeEach void open() { db = SqliteDatabase.open(tmp.resolve("test.db")); }

    @Test void roomKeepsOccupantsInOrder() {
        new SqliteRepositories.Students(db).save(new Student("S1", "Ayan", "1"));
        new SqliteRepositories.Students(db).save(new Student("S2", "Dana", "2"));
        var rooms = new SqliteRepositories.Rooms(db);
        Room r = new Room("R1", 2);
        r.assign("S2");
        r.assign("S1");
        rooms.save(r);
        assertEquals(List.of("S2", "S1"), rooms.findById("R1").orElseThrow().occupants());
    }

    @Test void undoStyleRestoreReplacesOccupants() {
        new SqliteRepositories.Students(db).save(new Student("S1", "Ayan", "1"));
        var rooms = new SqliteRepositories.Rooms(db);
        Room r = new Room("R1", 2);
        var before = r.save();
        r.assign("S1");
        rooms.save(r);
        r.restore(before);
        rooms.save(r);
        assertTrue(rooms.findById("R1").orElseThrow().occupants().isEmpty());
    }

    @Test void ticketStateSurvivesSaveAndLoad() {
        new SqliteRepositories.Rooms(db).save(new Room("R1", 1));
        var tickets = new SqliteRepositories.Tickets(db);
        MaintenanceTicket t = MaintenanceTicket.builder().id("T1").roomId("R1")
                .description("Leak").priority(Priority.URGENT).build();
        t.assignTo("Aibek");
        t.start();
        tickets.save(t);
        MaintenanceTicket loaded = tickets.findById("T1").orElseThrow();
        assertEquals("IN_PROGRESS", loaded.status());
        assertEquals("Aibek", loaded.technician());
        loaded.resolve();
        assertEquals("RESOLVED", loaded.status());
    }

    @Test void visitorDepartureIsStored() {
        new SqliteRepositories.Students(db).save(new Student("S1", "Ayan", "1"));
        var visitors = new SqliteRepositories.Visitors(db);
        VisitorEntry e = new VisitorEntry("V1", "Ali", "S1", LocalDateTime.of(2026, 11, 1, 10, 0));
        visitors.save(e);
        assertTrue(visitors.findById("V1").orElseThrow().isInside());
        e.depart(LocalDateTime.of(2026, 11, 1, 11, 0));
        visitors.save(e);
        assertFalse(visitors.findById("V1").orElseThrow().isInside());
    }

    @Test void databaseRejectsUnknownRoomForTicket() {
        var tickets = new SqliteRepositories.Tickets(db);
        MaintenanceTicket t = MaintenanceTicket.builder().id("T1").roomId("NOPE").description("x").build();
        assertThrows(IllegalStateException.class, () -> tickets.save(t));
    }
}
