package com.dorm.service;

import com.dorm.domain.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV export of the students it is given. It is fed the list that SecuredStudentService already
 * filtered for the caller's role, so staff get masked numbers in the file too.
 */
public final class StudentExporter {
    private StudentExporter() { }

    public static String toCsv(List<Student> students) {
        StringBuilder sb = new StringBuilder(Csv.row(List.of("id", "first_name", "last_name", "date_of_birth", "gender",
                "nationality", "iin", "national_id", "passport", "phone", "email", "faculty", "course_year", "address",
                "emergency_name", "emergency_phone"))).append('\n');
        for (Student s : students) {
            List<String> cells = new ArrayList<>();
            cells.add(s.id()); cells.add(s.firstName()); cells.add(s.lastName());
            cells.add(s.dateOfBirth() == null ? "" : s.dateOfBirth().toString());
            cells.add(s.gender()); cells.add(s.nationality()); cells.add(s.iin()); cells.add(s.nationalId());
            cells.add(s.passportNumber()); cells.add(s.phone()); cells.add(s.email()); cells.add(s.faculty());
            cells.add(s.courseYear() == null ? "" : String.valueOf(s.courseYear()));
            cells.add(s.address()); cells.add(s.emergencyName()); cells.add(s.emergencyPhone());
            sb.append(Csv.row(cells)).append('\n');
        }
        return sb.toString();
    }
}
