package com.uniflow.academic.student.infrastructure.persistence.postgres;

import com.uniflow.academic.student.application.ports.out.StudentRepository;
import com.uniflow.academic.student.domain.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentPostgresAdapter implements StudentRepository {

    private final StudentJpaRepository jpaRepository;
    private final StudentEntityMapper mapper;

    @Override
    public Student save(Student student) {
        log.debug("Saving new student {}", student.getEmail());
        StudentEntity entity = mapper.toEntity(student);
        StudentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Student update(Student student) {
        log.debug("Updating student {}", student.getId());
        StudentEntity entity = mapper.toEntity(student);
        StudentEntity updated = jpaRepository.save(entity);
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<Student> findByProvider(String provider, String providerId) {
        return jpaRepository.findByProviderAndProviderId(provider, providerId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Student> findById(String studentId) {
        return jpaRepository.findById(studentId)
                .map(mapper::toDomain);
    }
}
