package com.dorm.service;

import com.dorm.domain.Student;

import java.util.List;

/** What the interfaces may do with students. Implemented by StudentService and its security Proxy. */
public interface StudentOperations {
    Student register(StudentData data);
    List<Student> list();
    Student get(String id);
    Student update(String id, StudentData data);
    void delete(String id);
}
