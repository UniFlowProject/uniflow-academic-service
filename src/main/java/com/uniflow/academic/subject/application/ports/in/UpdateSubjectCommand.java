package com.uniflow.academic.subject.application.ports.in;

import com.uniflow.academic.subject.domain.Subject;

import java.util.List;
import java.util.Optional;

public interface UpdateSubjectCommand {

    Subject execute(String subjectId, UpdateSubjectRequest request, String studentId);

    record UpdateSubjectRequest(
            Optional<String> name,
            Optional<String> code,
            Optional<String> professor,
            Optional<Integer> credits,
            Optional<String> color,
            Optional<String> description
    ) {
        public UpdateSubjectRequest {

        }
    }
}
