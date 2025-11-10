package com.uniflow.academic.student.application.services;

import com.uniflow.academic.student.application.ports.in.CreateStudentCommand;
import com.uniflow.academic.student.application.ports.out.StudentRepository;
import com.uniflow.academic.student.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService implements CreateStudentCommand {

    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public Student execute(Student student) {
        student.validate();
        return studentRepository.save(student);
    }
}