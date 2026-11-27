package com.dorm.domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A resident. Identity numbers (iin, nationalId, passportNumber) are sensitive:
 * they are encrypted in the database and masked for staff (see SecuredStudentService).
 */
public record Student(String id, String firstName, String lastName, LocalDate dateOfBirth, String gender,
                      String nationality, String iin, String nationalId, String passportNumber,
                      String phone, String email, String faculty, Integer courseYear, String address,
                      String emergencyName, String emergencyPhone) implements Entity, Serializable {

    /** A student with only a name and phone (used by the console menu and tests). */
    public static Student basic(String id, String name, String phone) {
        String n = name == null ? "" : name.trim();
        int space = n.indexOf(' ');
        String first = space < 0 ? n : n.substring(0, space);
        String last = space < 0 ? "" : n.substring(space + 1).trim();
        return new Student(id, first, last, null, null, null, null, null, null, phone, null, null, null, null, null, null);
    }

    public String name() { return (firstName + " " + lastName).trim(); }

    /** Staff view: identity numbers are masked. */
    public Student masked() {
        return new Student(id, firstName, lastName, dateOfBirth, gender, nationality, Masking.mask(iin),
                Masking.mask(nationalId), Masking.mask(passportNumber), phone, email, faculty, courseYear,
                address, emergencyName, emergencyPhone);
    }

    /** Student view: a directory entry only (no personal documents, contacts or address). */
    public Student directoryView() {
        return new Student(id, firstName, lastName, null, null, null, null, null, null,
                null, null, faculty, courseYear, null, null, null);
    }
}
