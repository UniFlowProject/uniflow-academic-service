package com.uniflow.academic.course.infrastructure.web.dto.mapper;

import com.uniflow.academic.course.application.ports.in.CreateCourseCommand;
import com.uniflow.academic.course.application.ports.in.ReplaceCourseCommand;
import com.uniflow.academic.course.application.ports.in.UpdateCourseCommand;
import com.uniflow.academic.course.application.ports.out.dto.CourseStatisticsResponse;
import com.uniflow.academic.course.application.ports.out.dto.ValidateCourseCodeResponse;
import com.uniflow.academic.course.domain.Course;
import com.uniflow.academic.course.infrastructure.web.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CourseHttpMapper {

    public CreateCourseCommand.CreateCourseRequest toCreateCommandRequest(CreateCourseHttpRequest request) {
        return new CreateCourseCommand.CreateCourseRequest(
                request.getName(),
                request.getCode(),
                request.getProfessor(),
                request.getCredits(),
                request.getColor(),
                request.getPeriodId(),
                request.getDescription(),
                request.getSchedule()
        );
    }

    public ReplaceCourseCommand.ReplaceCourseRequest toReplaceCommandRequest(ReplaceCourseHttpRequest request) {
        return new ReplaceCourseCommand.ReplaceCourseRequest(
                request.getName(),
                request.getCode(),
                request.getProfessor(),
                request.getCredits(),
                request.getColor(),
                request.getDescription(),
                request.getSchedule()
        );
    }

    public UpdateCourseCommand.UpdateCourseRequest toUpdateCommandRequest(UpdateCourseHttpRequest request) {
        return new UpdateCourseCommand.UpdateCourseRequest(
                request.getName(),
                request.getCode(),
                request.getProfessor(),
                request.getCredits(),
                request.getColor(),
                request.getDescription()
        );
    }

    public CourseHttpResponse toHttpResponse(Course course) {
        return CourseHttpResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .professor(course.getProfessor())
                .credits(course.getCredits())
                .color(course.getColor())
                .periodId(course.getPeriodId())
                .description(course.getDescription())
                .schedule(course.getSchedule())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public UserCoursesHttpResponse toUserCoursesResponse(List<Course> courses) {
        List<CourseHttpResponse> data = courses.stream()
                .map(this::toHttpResponse)
                .collect(Collectors.toList());
        return UserCoursesHttpResponse.builder()
                .items(data)
                .build();
    }

    public CoursesByPeriodHttpResponse toCoursesByPeriodResponse(List<Course> courses) {
        List<CourseHttpResponse> data = courses.stream()
                .map(this::toHttpResponse)
                .collect(Collectors.toList());
        return CoursesByPeriodHttpResponse.builder()
                .items(data)
                .build();
    }

    public ValidateCourseCodeHttpResponse toValidateCodeResponse(ValidateCourseCodeResponse response) {
        return ValidateCourseCodeHttpResponse.builder()
                .isValid(response.isValid())
                .isAvailable(response.isAvailable())
                .message(response.getMessage())
                .build();
    }

    public CourseStatisticsHttpResponse toStatisticsHttpResponse(CourseStatisticsResponse response) {
        return CourseStatisticsHttpResponse.builder()
                .total(response.getTotal())
                .totalCredits(response.getTotalCredits())
                .averageCredits(response.getAverageCredits())
                .byProfessor(response.getByProfessor())
                .byCredits(response.getByCredits())
                .byPeriod(response.getByPeriod())
                .build();
    }
}
