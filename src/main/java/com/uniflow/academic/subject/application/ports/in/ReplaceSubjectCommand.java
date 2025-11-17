package com.uniflow.academic.subject.application.ports.in;

import com.uniflow.academic.subject.domain.Subject;

import java.util.List;

public interface ReplaceSubjectCommand {

    Subject execute(String subjectId, ReplaceSubjectRequest request, String studentId);

    record ReplaceSubjectRequest(
            String name,
            String code,
            String professor,
            Integer credits,
            String color,
            String description,
            List<String> schedule
    ) {
        public ReplaceSubjectRequest {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Subject name is required");
            }
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Subject code is required");
            }
        }
    }
}
