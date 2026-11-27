package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.infra.InMemoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificateServiceTest {
    private SessionContext session;
    private CertificateOperations secured;

    @BeforeEach void setUp() {
        InMemoryRepository<Student> students = new InMemoryRepository<>();
        InMemoryRepository<Room> rooms = new InMemoryRepository<>();
        students.save(new Student("S1", "Ayan", "<b>Nur</b>", LocalDate.of(2004, 5, 17), "MALE", "KZ", "040517501237",
                null, null, null, null, "IS", 2, null, null, null));
        students.save(Student.basic("S2", "Dana Serik", "1"));
        Room r = new Room("R101", 2);
        r.assign("S1");
        rooms.save(r);
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), ZoneOffset.UTC);
        session = new SessionContext(Role.STAFF);
        secured = new SecuredCertificateService(new CertificateService(students, rooms,
                List.of(new TextCertificateFormat(), new HtmlCertificateFormat(), new CsvCertificateFormat()), clock), session);
    }

    @Test void textCertificateNamesTheRoomAndDate() {
        IssuedDocument d = secured.issue("S1", "text");
        assertEquals("certificate-S1.txt", d.filename());
        assertTrue(d.content().contains("room R101"));
        assertTrue(d.content().contains("2026-11-26"));
        assertFalse(d.content().contains("0405175"));      // no identity numbers on a certificate
    }

    @Test void htmlEscapesNamesSoMarkupCannotBeInjected() {
        String html = secured.issue("S1", "HTML").content();
        assertTrue(html.contains("&lt;b&gt;Nur&lt;/b&gt;"));
        assertFalse(html.contains("<b>Nur</b>"));
    }

    @Test void csvHasHeaderAndOneRow() {
        String[] lines = secured.issue("S1", "csv").content().split("\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].startsWith("S1,"));
        assertTrue(lines[1].contains("R101"));
    }

    @Test void rulesAreEnforced() {
        assertThrows(IllegalStateException.class, () -> secured.issue("S2", "text"));       // no room
        assertThrows(NotFoundException.class, () -> secured.issue("nobody", "text"));
        assertThrows(IllegalArgumentException.class, () -> secured.issue("S1", "pdf"));
        assertEquals(List.of("text", "html", "csv"), secured.formats());
        session.switchTo(Role.STUDENT);
        assertThrows(AccessDeniedException.class, () -> secured.issue("S1", "text"));
    }

    @Test void csvCellsCannotRunAsSpreadsheetFormulas() {
        assertEquals("'=SUM(A1)", Csv.cell("=SUM(A1)"));
        assertEquals("\"a,b\"", Csv.cell("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", Csv.cell("say \"hi\""));
        assertEquals("", Csv.cell(null));
        assertEquals("plain", Csv.cell("plain"));
    }

    @Test void studentExportKeepsWhatItIsGiven() {
        Student s = new Student("S1", "Ayan", "Nur", LocalDate.of(2004, 5, 17), "MALE", "KZ", "********1237",
                null, null, "777", "a@b.kz", "IS", 2, "Street, 1", null, null);
        String csv = StudentExporter.toCsv(List.of(s));
        assertTrue(csv.startsWith("id,first_name"));
        assertTrue(csv.contains("********1237"));
        assertTrue(csv.contains("\"Street, 1\""));
    }
}
