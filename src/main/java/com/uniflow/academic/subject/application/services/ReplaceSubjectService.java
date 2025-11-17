package com.uniflow.academic.subject.application.services;

import com.uniflow.academic.subject.application.ports.in.ReplaceSubjectCommand;
import com.uniflow.academic.subject.application.ports.out.SubjectRepository;
import com.uniflow.academic.subject.domain.Subject;
import com.uniflow.academic.subject.domain.exception.SubjectCodeAlreadyExistsException;
import com.uniflow.academic.subject.domain.exception.SubjectNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReplaceSubjectService implements ReplaceSubjectCommand {

    private final SubjectRepository subjectRepository;

    @Override
    public Subject execute(String subjectId, ReplaceSubjectRequest request, String studentId) {
        log.info("Updating subject {} for student {}", subjectId, studentId);

        Subject current = subjectRepository.findById(subjectId, studentId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));

        if (subjectRepository.existsByCode(request.code(), current.getPeriodId(), studentId, subjectId)) {
            throw new SubjectCodeAlreadyExistsException("Subject code already exists in this period");
        }

        List<String> schedule = request.schedule() != null ? request.schedule() : List.of();

        Subject updated = current.update(
                request.name(),
                request.code(),
                request.professor(),
                request.credits(),
                request.color(),
                request.description(),
                schedule
        );

        Subject saved = subjectRepository.update(updated);
        log.info("Subject {} updated successfully", saved.getId());
        return saved;
    }
}
