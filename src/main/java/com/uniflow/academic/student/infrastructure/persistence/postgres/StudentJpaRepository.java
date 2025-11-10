package com.uniflow.academic.student.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, String> {

    Optional<StudentEntity> findByProviderAndProviderId(String provider, String providerId);
}
