package com.dorm.service;

import java.util.List;

public class CsvCertificateFormat implements CertificateFormat {
    @Override public String id() { return "csv"; }
    @Override public String contentType() { return "text/csv; charset=utf-8"; }
    @Override public String extension() { return "csv"; }

    @Override public String render(AccommodationCertificate c) {
        return Csv.row(List.of("student_id", "name", "faculty", "course_year", "room", "issued_on")) + "\n"
                + Csv.row(List.of(c.studentId(), c.studentName(), c.faculty() == null ? "" : c.faculty(),
                    c.courseYear() == null ? "" : String.valueOf(c.courseYear()), c.roomId(), c.issuedOn().toString())) + "\n";
    }
}
