package com.uniflow.academic.subject.application.ports.in;

import com.uniflow.academic.subject.domain.Subject;

import java.util.List;
import java.util.Optional;

public interface UpdateSubjectCommand {

    Subject execute(String subjectId, UpdateSubjectRequest request, String studentId);

    record UpdateSubjectRequest(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description
    ) {
        public UpdateSubjectRequest {

        }
    }
}
