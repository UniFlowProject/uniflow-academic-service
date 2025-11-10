package com.uniflow.academic.student.application.ports.in;

import com.uniflow.academic.student.domain.Student;

public interface CreateStudentCommand {
    Student execute(Student student);
}