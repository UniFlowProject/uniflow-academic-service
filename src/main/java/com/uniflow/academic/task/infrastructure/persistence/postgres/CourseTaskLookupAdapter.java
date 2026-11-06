package com.uniflow.academic.task.infrastructure.persistence.postgres;

import com.uniflow.academic.course.application.ports.out.CourseTaskLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseTaskLookupAdapter implements CourseTaskLookup {

    private final TaskJpaRepository jpaRepository;

    @Override
    public boolean existsByCourseId(String courseId, String userId) {
        return jpaRepository.existsByCourseIdAndUserId(courseId, userId);
    }
}
