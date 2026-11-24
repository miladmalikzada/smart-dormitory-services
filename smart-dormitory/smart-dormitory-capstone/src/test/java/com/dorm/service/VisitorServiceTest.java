package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Student;
import com.dorm.domain.VisitorEntry;
import com.dorm.infra.InMemoryRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class VisitorServiceTest {
    @Test void visitorMustHaveARegisteredHost() {
        VisitorService s = new VisitorService(new InMemoryRepository<>(), new InMemoryRepository<>(),
                new EventPublisher(), Clock.systemUTC());
        assertThrows(IllegalArgumentException.class, () -> s.arrive("Ali", "nobody"));
    }

    @Test void arrivalAndDepartureAreTracked() {
        InMemoryRepository<Student> students = new InMemoryRepository<>();
        students.save(new Student("S1", "Host", "1"));
        VisitorService s = new VisitorService(new InMemoryRepository<>(), students, new EventPublisher(), Clock.systemUTC());
        VisitorEntry e = s.arrive("Ali", "S1");
        assertTrue(s.log().inside().iterator().hasNext());
        s.depart(e.id());
        assertFalse(s.log().inside().iterator().hasNext());
    }
}
