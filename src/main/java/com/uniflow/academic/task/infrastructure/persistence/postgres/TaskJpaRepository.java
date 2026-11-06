package com.uniflow.academic.task.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, String>, JpaSpecificationExecutor<TaskEntity> {

    Optional<TaskEntity> findByIdAndUserId(String id, String userId);

    @Query("select t.status, count(t) from TaskEntity t where t.userId = :userId group by t.status")
    List<Object[]> countByStatus(@Param("userId") String userId);

    @Query("select t.priority, count(t) from TaskEntity t where t.userId = :userId group by t.priority")
    List<Object[]> countByPriority(@Param("userId") String userId);

    @Query("select t.type, count(t) from TaskEntity t where t.userId = :userId group by t.type")
    List<Object[]> countByType(@Param("userId") String userId);

    @Query("select t.courseId, count(t) from TaskEntity t where t.userId = :userId group by t.courseId")
    List<Object[]> countByCourse(@Param("userId") String userId);

    @Query("""
            select count(t) from TaskEntity t
            where t.userId = :userId and t.status not in ('done', 'cancelled') and t.dueDate < :now
            """)
    long countOverdue(@Param("userId") String userId, @Param("now") Instant now);

    boolean existsByCourseIdAndUserId(String courseId, String userId);

    long deleteByIdAndUserId(String id, String userId);
}
