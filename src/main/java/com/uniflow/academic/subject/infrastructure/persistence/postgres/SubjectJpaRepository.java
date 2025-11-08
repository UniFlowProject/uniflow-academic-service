package com.uniflow.academic.subject.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, String> {

    Optional<SubjectEntity> findByIdAndStudentId(String id, String studentId);

    @Query("""
            SELECT s FROM SubjectEntity s
            WHERE s.studentId = :studentId
            AND (:periodId IS NULL OR s.periodId = :periodId)
            AND (:professor IS NULL OR LOWER(s.professor) LIKE LOWER(CONCAT('%', :professor, '%')))
            AND (:credits IS NULL OR s.credits = :credits)
            AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.createdAt DESC
            """)
    List<SubjectEntity> searchSubjects(
            @Param("studentId") String studentId,
            @Param("periodId") String periodId,
            @Param("professor") String professor,
            @Param("credits") Integer credits,
            @Param("search") String search
        SELECT s FROM SubjectEntity s
        WHERE s.studentId = :studentId
        AND (:periodId IS NULL OR s.periodId = :periodId)
        AND (:professorPattern IS NULL OR LOWER(s.professor) LIKE :professorPattern)
        AND (:credits IS NULL OR s.credits = :credits)
        AND (:searchPattern IS NULL OR LOWER(s.name) LIKE :searchPattern
             OR LOWER(s.code) LIKE :searchPattern)
        ORDER BY s.createdAt DESC
    """)
    List<SubjectEntity> searchSubjects(
            @Param("studentId") String studentId,
            @Param("periodId") String periodId,
            @Param("professorPattern") String professorPattern,
            @Param("credits") Integer credits,
            @Param("searchPattern") String searchPattern
    );

    List<SubjectEntity> findByStudentIdAndPeriodId(String studentId, String periodId);

    boolean existsByStudentIdAndPeriodIdAndCodeIgnoreCase(String studentId, String periodId, String code);

    boolean existsByStudentIdAndPeriodIdAndCodeIgnoreCaseAndIdNot(
            String studentId,
            String periodId,
            String code,
            String id
    );

    long countByPeriodId(String periodId);

    long countByPeriodIdAndStudentId(String periodId, String studentId);

    List<SubjectEntity> findByStudentId(String studentId);
}
