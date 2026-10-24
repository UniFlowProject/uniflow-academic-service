package com.uniflow.academic.course.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, String> {

    Optional<CourseEntity> findByIdAndUserId(String id, String userId);

    @Query("""
        SELECT s FROM CourseEntity s
        WHERE s.userId = :userId
        AND (:periodId IS NULL OR s.periodId = :periodId)
        AND (:professorPattern IS NULL OR LOWER(s.professor) LIKE :professorPattern)
        AND (:credits IS NULL OR s.credits = :credits)
        AND (:searchPattern IS NULL OR LOWER(s.name) LIKE :searchPattern
             OR LOWER(s.code) LIKE :searchPattern)
        ORDER BY s.createdAt DESC
    """)
    List<CourseEntity> searchCourses(
            @Param("userId") String userId,
            @Param("periodId") String periodId,
            @Param("professorPattern") String professorPattern,
            @Param("credits") Integer credits,
            @Param("searchPattern") String searchPattern
    );

    List<CourseEntity> findByUserIdAndPeriodId(String userId, String periodId);

    boolean existsByUserIdAndPeriodIdAndCodeIgnoreCase(String userId, String periodId, String code);

    boolean existsByUserIdAndPeriodIdAndCodeIgnoreCaseAndIdNot(
            String userId,
            String periodId,
            String code,
            String id
    );

    long countByPeriodId(String periodId);

    long countByPeriodIdAndUserId(String periodId, String userId);

    List<CourseEntity> findByUserId(String userId);
}
