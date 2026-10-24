package com.uniflow.academic.course.application.ports.out.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CourseFilter {
    String periodId;
    String professor;
    Integer credits;
    String search;

    public boolean isEmpty() {
        return (periodId == null || periodId.isBlank())
                && (professor == null || professor.isBlank())
                && credits == null
                && (search == null || search.isBlank());
    }
}
