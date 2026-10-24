package com.uniflow.academic.course.infrastructure.persistence.postgres;

import com.uniflow.academic.course.application.ports.out.CourseRepository;
import com.uniflow.academic.course.application.ports.out.dto.CourseFilter;
import com.uniflow.academic.course.application.ports.out.dto.CourseStatisticsResponse;
import com.uniflow.academic.course.domain.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoursePostgresAdapter implements CourseRepository {

    private final CourseJpaRepository jpaRepository;
    private final CourseEntityMapper mapper;

    @Override
    public Course save(Course course) {
        log.debug("Saving course {}", course.getId());
        CourseEntity entity = mapper.toEntity(course);
        CourseEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Course update(Course course) {
        log.debug("Updating course {}", course.getId());
        CourseEntity entity = mapper.toEntity(course);
        CourseEntity updated = jpaRepository.save(entity);
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<Course> findById(String courseId, String userId) {
        return jpaRepository.findByIdAndUserId(courseId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Course> findAll(String userId, CourseFilter filter) {
        String periodId = filter != null ? filter.getPeriodId() : null;
        String professor = filter != null ? filter.getProfessor() : null;
        Integer credits = filter != null ? filter.getCredits() : null;
        String search = filter != null ? filter.getSearch() : null;

        List<CourseEntity> entities = jpaRepository.searchCourses(
                userId,
                normalize(periodId),
                normalize(professor),
                credits,
                normalize(search)
        );

        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Course> findByPeriodId(String periodId, String userId) {
        return jpaRepository.findByUserIdAndPeriodId(userId, periodId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String code, String periodId, String userId, String excludeCourseId) {
        if (excludeCourseId != null) {
            return jpaRepository.existsByUserIdAndPeriodIdAndCodeIgnoreCaseAndIdNot(
                    userId,
                    periodId,
                    code,
                    excludeCourseId
            );
        }
        return jpaRepository.existsByUserIdAndPeriodIdAndCodeIgnoreCase(userId, periodId, code);
    }

    @Override
    @Transactional
    public boolean delete(String courseId, String userId) {
        return jpaRepository.deleteByIdAndUserId(courseId, userId) > 0;
    }

    @Override
    public boolean existsByPeriodId(String periodId, String userId) {
        return jpaRepository.existsByPeriodIdAndUserId(periodId, userId);
    }

    @Override
    public boolean hasAssociatedTasks(String courseId) {
        // Tasks module not implemented yet.
        return false;
    }

    @Override
    public CourseStatisticsResponse getStatistics(String userId) {
        List<CourseEntity> entities = jpaRepository.findByUserId(userId);
        if (entities.isEmpty()) {
            return CourseStatisticsResponse.builder()
                    .total(0)
                    .totalCredits(0)
                    .averageCredits(0.0)
                    .byProfessor(Collections.emptyMap())
                    .byCredits(Collections.emptyMap())
                    .byPeriod(Collections.emptyMap())
                    .build();
        }

        List<Course> courses = entities.stream()
                .map(mapper::toDomain)
                .toList();

        int total = courses.size();
        int totalCredits = courses.stream()
                .map(Course::getCredits)
                .filter(credits -> credits != null)
                .mapToInt(Integer::intValue)
                .sum();

        double averageCredits = total == 0 ? 0.0 : (double) totalCredits / total;

        var byProfessor = courses.stream()
                .collect(Collectors.groupingBy(
                        course -> course.getProfessor() != null ? course.getProfessor() : "Unknown",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        var byCredits = courses.stream()
                .filter(course -> course.getCredits() != null)
                .collect(Collectors.groupingBy(
                        Course::getCredits,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        var byPeriod = courses.stream()
                .collect(Collectors.groupingBy(
                        Course::getPeriodId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return CourseStatisticsResponse.builder()
                .total(total)
                .totalCredits(totalCredits)
                .averageCredits(averageCredits)
                .byProfessor(byProfessor)
                .byCredits(byCredits)
                .byPeriod(byPeriod)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
