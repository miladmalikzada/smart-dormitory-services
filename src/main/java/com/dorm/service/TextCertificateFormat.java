package com.dorm.service;

public class TextCertificateFormat implements CertificateFormat {
    @Override public String id() { return "text"; }
    @Override public String contentType() { return "text/plain; charset=utf-8"; }
    @Override public String extension() { return "txt"; }

    @Override public String render(AccommodationCertificate c) {
        return "ACCOMMODATION CERTIFICATE\n"
                + "=========================\n"
                + "This certifies that " + c.studentName() + " (" + c.studentId() + ")\n"
                + (c.faculty() == null ? "" : "Faculty: " + c.faculty()
                    + (c.courseYear() == null ? "" : ", year " + c.courseYear()) + "\n")
                + "is a resident of the university dormitory, room " + c.roomId() + ".\n\n"
                + "Issued on " + c.issuedOn() + " by Smart Dormitory Services.\n";
    }
}
