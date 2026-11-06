package com.uniflow.academic.period.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodJpaRepository
        extends JpaRepository<PeriodEntity, String> {

    /**
     * Find a period by ID and user ID
     */
    Optional<PeriodEntity> findByIdAndUserId(
            String id,
            String userId
    );

    /**
     * Find all periods for a user
     */
    Page<PeriodEntity> findByUserIdOrderByStartDateDesc(
            String userId,
            Pageable pageable
    );

    List<PeriodEntity> findAllByUserId(String userId);

    /**
     * Find all periods with multiple filters
     */
    @Query(
            """
            SELECT p FROM PeriodEntity p
            WHERE p.userId = :userId
            AND (:type IS NULL OR p.type = :type)
            AND (:year IS NULL OR p.year = :year)
            AND (:isActive IS NULL OR p.isActive = :isActive)
            ORDER BY p.startDate DESC
            """
    )
    Page<PeriodEntity> findAllWithFilters(
            @Param("userId") String userId,
            @Param("type") String type,
            @Param("year") Integer year,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    /**
     * Find the current active period for a user
     */
    Optional<PeriodEntity> findByUserIdAndIsActive(
            String userId,
            Boolean isActive
    );

    /**
     * Find all active periods for a user
     */
    List<PeriodEntity> findAllByUserIdAndIsActive(
            String userId,
            Boolean isActive
    );

    /**
     * Check if period exists for user
     */
    boolean existsByIdAndUserId(String id, String userId);

    long deleteByIdAndUserId(String id, String userId);
}