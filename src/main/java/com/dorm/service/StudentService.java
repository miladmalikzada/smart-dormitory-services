package com.dorm.service;

import com.dorm.domain.Iin;
import com.dorm.domain.Repository;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Full CRUD for students, with validation and the rules that protect related data. */
public class StudentService implements StudentOperations {
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE");
    private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final java.util.regex.Pattern DOC = java.util.regex.Pattern.compile("^[A-Za-z0-9]{5,20}$");

    private final Repository<Student> students;
    private final Repository<Room> rooms;
    private final Repository<VisitorEntry> visitors;

    public StudentService(Repository<Student> students, Repository<Room> rooms, Repository<VisitorEntry> visitors) {
        this.students = students;
        this.rooms = rooms;
        this.visitors = visitors;
    }

    @Override public Student register(StudentData d) {
        Student s = build(Ids.next("S"), d, null);
        students.save(s);
        return s;
    }

    @Override public List<Student> list() { return students.findAll(); }

    @Override public Student get(String id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Unknown student " + id));
    }

    /** Identity numbers left out (null) of an update keep their stored value. */
    @Override public Student update(String id, StudentData d) {
        Student old = get(id);
        Student s = build(id, d, old);
        students.save(s);
        return s;
    }

    @Override public void delete(String id) {
        get(id);
        if (rooms.findAll().stream().anyMatch(r -> r.houses(id))) {
            throw new IllegalStateException("Student lives in a room; check them out first");
        }
        if (visitors.findAll().stream().anyMatch(v -> v.hostStudentId().equals(id))) {
            throw new IllegalStateException("Student has visitor records; delete those first");
        }
        students.delete(id);
    }

    private Student build(String id, StudentData d, Student old) {
        String first = required(d.firstName(), "first name");
        String last = required(d.lastName(), "last name");
        LocalDate dob = parseDate(required(d.dateOfBirth(), "date of birth"));
        if (dob.isAfter(LocalDate.now())) throw new IllegalArgumentException("date of birth cannot be in the future");

        String iin = keep(d.iin(), old == null ? null : old.iin());
        if (iin == null) throw new IllegalArgumentException("IIN is required");
        if (!Iin.isValid(iin)) throw new IllegalArgumentException("IIN is not valid (12 digits with a correct check digit)");
        if (!dob.equals(Iin.birthDate(iin).orElseThrow())) {
            throw new IllegalArgumentException("date of birth does not match the IIN");
        }
        String nationalId = document(keep(d.nationalId(), old == null ? null : old.nationalId()), "national ID number");
        String passport = document(keep(d.passportNumber(), old == null ? null : old.passportNumber()), "passport number");

        String gender = blankToNull(d.gender());
        if (gender != null) {
            gender = gender.toUpperCase();
            if (!GENDERS.contains(gender)) throw new IllegalArgumentException("gender must be MALE or FEMALE");
        }
        String email = blankToNull(d.email());
        if (email != null && !EMAIL.matcher(email).matches()) throw new IllegalArgumentException("email is not valid");
        Integer year = d.courseYear();
        if (year != null && (year < 1 || year > 8)) throw new IllegalArgumentException("course year must be 1-8");

        unique(id, "IIN", iin, Student::iin);
        unique(id, "national ID number", nationalId, Student::nationalId);
        unique(id, "passport number", passport, Student::passportNumber);

        return new Student(id, first, last, dob, gender, blankToNull(d.nationality()), iin, nationalId, passport,
                blankToNull(d.phone()), email, blankToNull(d.faculty()), year, blankToNull(d.address()),
                blankToNull(d.emergencyName()), blankToNull(d.emergencyPhone()));
    }

    private void unique(String selfId, String label, String value, Function<Student, String> field) {
        if (value == null) return;
        boolean taken = students.findAll().stream()
                .anyMatch(o -> !o.id().equals(selfId) && value.equalsIgnoreCase(field.apply(o)));
        if (taken) throw new IllegalStateException("A student with this " + label + " already exists");
    }

    private static String keep(String given, String stored) {
        return given == null || given.isBlank() ? stored : given.trim();
    }

    private static String document(String value, String label) {
        if (value == null) return null;
        if (!DOC.matcher(value).matches()) throw new IllegalArgumentException(label + " must be 5-20 letters or digits");
        return value.toUpperCase();
    }

    private static String required(String v, String label) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(label + " is required");
        return v.trim();
    }

    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    private static LocalDate parseDate(String v) {
        try { return LocalDate.parse(v); }
        catch (DateTimeException e) { throw new IllegalArgumentException("date of birth must look like 2004-05-17"); }
    }
}
