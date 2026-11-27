package com.dorm.service;

import com.dorm.domain.Repository;
import com.dorm.domain.Room;
import com.dorm.domain.Student;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Issues accommodation certificates in any registered format (Strategy). */
public class CertificateService implements CertificateOperations {
    private final Repository<Student> students;
    private final Repository<Room> rooms;
    private final Map<String, CertificateFormat> formats = new LinkedHashMap<>();
    private final Clock clock;

    public CertificateService(Repository<Student> students, Repository<Room> rooms,
                              List<CertificateFormat> formats, Clock clock) {
        this.students = students;
        this.rooms = rooms;
        this.clock = clock;
        for (CertificateFormat f : formats) this.formats.put(f.id(), f);
    }

    @Override public List<String> formats() { return List.copyOf(formats.keySet()); }

    @Override public IssuedDocument issue(String studentId, String formatId) {
        CertificateFormat format = formats.get(formatId == null ? "" : formatId.toLowerCase());
        if (format == null) throw new IllegalArgumentException("unknown format; choose one of " + formats.keySet());
        Student s = students.findById(studentId).orElseThrow(() -> new NotFoundException("Unknown student " + studentId));
        Room room = rooms.findAll().stream().filter(r -> r.houses(studentId)).findFirst()
                .orElseThrow(() -> new IllegalStateException(s.name() + " does not have a room yet"));
        AccommodationCertificate c = new AccommodationCertificate(s.id(), s.name(), s.faculty(), s.courseYear(),
                room.id(), LocalDate.now(clock));
        return new IssuedDocument("certificate-" + s.id() + "." + format.extension(), format.contentType(), format.render(c));
    }
}
