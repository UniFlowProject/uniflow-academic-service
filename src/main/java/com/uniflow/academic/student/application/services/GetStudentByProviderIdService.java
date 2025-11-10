package com.uniflow.academic.student.application.services;

import com.uniflow.academic.student.application.ports.in.GetStudentByProviderIdQuery;
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
public class GetStudentByProviderIdService implements GetStudentByProviderIdQuery {

    private final StudentRepository studentRepository;

    @Override
    public Student getByProviderId(String providerId) {
        log.info("Retrieving student by providerId {}", providerId);
        return studentRepository.findByProvider("google", providerId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found"));
    }

    @Override
    public Student execute(String providerId) {
        log.info("Retrieving student by providerId {}", providerId);
        return studentRepository.findByProvider("google", providerId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found"));
    }
}
