package com.uniflow.academic.subject.application.services;

import com.uniflow.academic.subject.application.ports.in.ReplaceSubjectCommand;
import com.uniflow.academic.subject.application.ports.in.UpdateSubjectCommand;
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
public class UpdateSubjectService implements UpdateSubjectCommand {

    private final SubjectRepository subjectRepository;

    @Override
    public Subject execute(String subjectId, UpdateSubjectRequest request, String studentId) {
        log.info("Updating subject {} for student {}", subjectId, studentId);

        Subject current = subjectRepository.findById(subjectId, studentId)
                .orElseThrow(() -> new SubjectNotFoundException("Subject not found"));

        if (request.code() != null) {
            if(subjectRepository.existsByCode(request.code(), current.getPeriodId(), studentId, subjectId)) {
                throw new SubjectCodeAlreadyExistsException("Subject code already exists in this period");
            }
        }

        Subject updated = current.patch(
                request.name(),
                request.code(),
                request.professor(),
                request.credits(),
                request.color(),
                request.description()
        );

        Subject saved = subjectRepository.update(updated);
        log.info("Subject {} updated successfully", saved.getId());
        return saved;
    }
}
