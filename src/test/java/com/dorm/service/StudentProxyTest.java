package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Role;
import com.dorm.domain.Room;
import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.infra.InMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentProxyTest {
    private SessionContext session;
    private StudentOperations secured;
    private StudentData data;

    @BeforeEach void setUp() {
        session = new SessionContext(Role.ADMIN);
        secured = new SecuredStudentService(new StudentService(new InMemoryRepository<Student>(),
                new InMemoryRepository<Room>(), new InMemoryRepository<VisitorEntry>()), session);
        data = new StudentData("Ayan", "Nur", "2004-05-17", "MALE", "KZ", "040517501237", "046123456", "N12345678",
                "777", "a@b.kz", "IS", 2, "Street 1", "Mom", "888");
    }

    @Test void adminSeesEverythingStaffSeesMaskedStudentSeesDirectory() {
        String id = secured.register(data).id();
        assertEquals("040517501237", secured.get(id).iin());
        session.switchTo(Role.STAFF);
        assertEquals("********1237", secured.get(id).iin());
        assertEquals("777", secured.list().get(0).phone());
        session.switchTo(Role.STUDENT);
        assertNull(secured.get(id).iin());
        assertNull(secured.list().get(0).email());
        assertEquals("IS", secured.get(id).faculty());
    }

    @Test void onlyAdminWrites() {
        session.switchTo(Role.STAFF);
        assertThrows(AccessDeniedException.class, () -> secured.register(data));
        assertThrows(AccessDeniedException.class, () -> secured.delete("S-1"));
        session.switchTo(Role.STUDENT);
        assertThrows(AccessDeniedException.class, () -> secured.update("S-1", data));
    }

    @Test void serviceRulesStillApplyBehindTheProxy() {
        secured.register(data);
        assertThrows(IllegalStateException.class, () -> secured.register(data));      // duplicate IIN
        assertThrows(IllegalArgumentException.class, () -> secured.register(
                new StudentData("A", "B", "2004-05-17", "X", null, "040517501237", null, null, null, null, null, null, null, null, null)));
        assertThrows(NotFoundException.class, () -> secured.get("nobody"));
    }
}
