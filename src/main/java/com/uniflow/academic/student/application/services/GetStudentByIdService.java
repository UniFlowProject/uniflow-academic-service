package com.uniflow.academic.student.application.services;

import com.uniflow.academic.student.application.ports.in.GetStudentByIdQuery;
import com.uniflow.academic.student.application.ports.out.StudentRepository;
import com.uniflow.academic.student.domain.Student;
import com.uniflow.academic.student.domain.exception.StudentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStudentByIdService implements GetStudentByIdQuery {

    private final StudentRepository studentRepository;

    @Override
    public Student getById(String studentId) {
        log.info("Retrieving student {}", studentId);
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found"));
    }
}
