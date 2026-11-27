package com.dorm.service;

import com.dorm.domain.Role;
import com.dorm.domain.Student;

import java.util.List;

/**
 * Protection Proxy (L11) for personal data: ADMIN sees everything and may change students,
 * STAFF sees identity numbers masked, STUDENT sees only a directory entry. The real service has no role code.
 */
public class SecuredStudentService implements StudentOperations {
    private final StudentOperations target;
    private final SessionContext session;

    public SecuredStudentService(StudentOperations target, SessionContext session) {
        this.target = target;
        this.session = session;
    }

    @Override public Student register(StudentData data) { return view(target.register(requireAdmin(data))); }
    @Override public Student update(String id, StudentData data) { return view(target.update(id, requireAdmin(data))); }
    @Override public void delete(String id) { requireAdmin(null); target.delete(id); }
    @Override public List<Student> list() { return target.list().stream().map(this::view).toList(); }
    @Override public Student get(String id) { return view(target.get(id)); }

    private Student view(Student s) {
        Role role = session.role();
        return switch (role) {
            case ADMIN -> s;
            case STAFF -> s.masked();
            case STUDENT -> s.directoryView();
        };
    }

    private StudentData requireAdmin(StudentData data) {
        if (session.role() != Role.ADMIN) throw new AccessDeniedException("Only an administrator can change students");
        return data;
    }
}
