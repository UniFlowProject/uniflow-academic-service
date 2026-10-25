package com.uniflow.academic.task.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, String>, JpaSpecificationExecutor<TaskEntity> {

    Optional<TaskEntity> findByIdAndUserId(String id, String userId);

    List<TaskEntity> findByUserId(String userId);

    boolean existsByCourseIdAndUserId(String courseId, String userId);

    long deleteByIdAndUserId(String id, String userId);
}
