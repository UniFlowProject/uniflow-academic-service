package com.uniflow.academic.subject.infrastructure.web.dto.mapper;

import com.uniflow.academic.subject.application.ports.in.CreateSubjectCommand;
import com.uniflow.academic.subject.application.ports.in.ReplaceSubjectCommand;
import com.uniflow.academic.subject.application.ports.in.UpdateSubjectCommand;
import com.uniflow.academic.subject.application.ports.out.dto.SubjectStatisticsResponse;
import com.uniflow.academic.subject.application.ports.out.dto.ValidateSubjectCodeResponse;
import com.uniflow.academic.subject.domain.Subject;
import com.uniflow.academic.subject.infrastructure.web.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectHttpMapper {

    public CreateSubjectCommand.CreateSubjectRequest toCreateCommandRequest(CreateSubjectHttpRequest request) {
        return new CreateSubjectCommand.CreateSubjectRequest(
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

    public ReplaceSubjectCommand.ReplaceSubjectRequest toReplaceCommandRequest(ReplaceSubjectHttpRequest request) {
        return new ReplaceSubjectCommand.ReplaceSubjectRequest(
                request.getName(),
                request.getCode(),
                request.getProfessor(),
                request.getCredits(),
                request.getColor(),
                request.getDescription(),
                request.getSchedule()
        );
    }

    public UpdateSubjectCommand.UpdateSubjectRequest toUpdateCommandRequest(UpdateSubjectHttpRequest request) {
        return new UpdateSubjectCommand.UpdateSubjectRequest(
                request.getName(),
                request.getCode(),
                request.getProfessor(),
                request.getCredits(),
                request.getColor(),
                request.getDescription()
        );
    }

    public SubjectHttpResponse toHttpResponse(Subject subject) {
        return SubjectHttpResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .professor(subject.getProfessor())
                .credits(subject.getCredits())
                .color(subject.getColor())
                .periodId(subject.getPeriodId())
                .description(subject.getDescription())
                .schedule(subject.getSchedule())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }

    public UserSubjectsHttpResponse toUserSubjectsResponse(List<Subject> subjects) {
        List<SubjectHttpResponse> data = subjects.stream()
                .map(this::toHttpResponse)
                .collect(Collectors.toList());
        return UserSubjectsHttpResponse.builder()
                .data(data)
                .build();
    }

    public SubjectsByPeriodHttpResponse toSubjectsByPeriodResponse(List<Subject> subjects) {
        List<SubjectHttpResponse> data = subjects.stream()
                .map(this::toHttpResponse)
                .collect(Collectors.toList());
        return SubjectsByPeriodHttpResponse.builder()
                .subjects(data)
                .build();
    }

    public ValidateSubjectCodeHttpResponse toValidateCodeResponse(ValidateSubjectCodeResponse response) {
        return ValidateSubjectCodeHttpResponse.builder()
                .isValid(response.isValid())
                .isAvailable(response.isAvailable())
                .message(response.getMessage())
                .build();
    }

    public SubjectStatisticsHttpResponse toStatisticsHttpResponse(SubjectStatisticsResponse response) {
        return SubjectStatisticsHttpResponse.builder()
                .total(response.getTotal())
                .totalCredits(response.getTotalCredits())
                .averageCredits(response.getAverageCredits())
                .byProfessor(response.getByProfessor())
                .byCredits(response.getByCredits())
                .byPeriod(response.getByPeriod())
                .build();
    }
}
