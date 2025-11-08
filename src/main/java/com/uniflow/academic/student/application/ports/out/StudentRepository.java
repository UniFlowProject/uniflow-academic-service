package com.uniflow.academic.student.application.ports.out;

import com.uniflow.academic.student.domain.Student;

import java.util.Optional;

public interface StudentRepository {

    Student save(Student student);

    Student update(Student student);

    Optional<Student> findByProvider(String provider, String providerId);

    Optional<Student> findById(String studentId);
}
