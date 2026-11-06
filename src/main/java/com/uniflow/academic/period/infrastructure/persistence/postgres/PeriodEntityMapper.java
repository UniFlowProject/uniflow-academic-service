package com.uniflow.academic.period.infrastructure.persistence.postgres;

import com.uniflow.academic.period.domain.Period;
import org.springframework.stereotype.Component;

/**
 * Mapper between PeriodEntity (JPA) and Period (Domain).
 * Converts between persistence and domain representations.
 */
@Component
public class PeriodEntityMapper {

    /**
     * Convert domain Period to JPA Entity
     */
    public PeriodEntity toEntity(Period period) {
        if (period == null) {
            return null;
        }

        return PeriodEntity.builder()
                .id(period.getId())
                .name(period.getName())
                .type(period.getType().getValue())
                .year(period.getYear())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .userId(period.getUserId())
                .isActive(period.getIsActive())
                .createdAt(period.getCreatedAt())
                .updatedAt(period.getUpdatedAt())
                .build();
    }

    /**
     * Convert JPA Entity to domain Period
     */
    public Period toDomain(PeriodEntity entity) {
        if (entity == null) {
            return null;
        }

        return Period.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(Period.PeriodType.fromValue(entity.getType()))
                .year(entity.getYear())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .userId(entity.getUserId())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}