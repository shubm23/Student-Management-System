package com.example.sms.service;

import com.example.sms.dto.SubjectRequest;
import com.example.sms.dto.SubjectResponse;
import com.example.sms.entity.Subject;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.exception.SubjectInUseException;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;

    public SubjectService(SubjectRepository subjectRepository, StudentRepository studentRepository) {
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public SubjectResponse addSubject(SubjectRequest request) {
        String name = request.getName();
        ensureNameIsAvailable(name);

        Subject saved = subjectRepository.save(new Subject(name));
        return SubjectResponse.from(saved);
    }

    @Transactional
    public void deleteSubject(Long subjectId) {
        Subject subject = subjectRepository.findByIdForUpdate(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("No subject found with id: " + subjectId));

        long assignedCount = studentRepository.countStudentsBySubjectId(subjectId);
        if (assignedCount > 0) {
            throw new SubjectInUseException(
                    "Cannot delete " + subject.getName() + " because "
                            + assignedCount + " " + studentLabel(assignedCount) + " already assigned to it");
        }
        subjectRepository.delete(subject);
        subjectRepository.flush();
    }

    private void ensureNameIsAvailable(String name) {
        if (subjectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A subject with this name already exists: " + name);
        }
    }

    private static String studentLabel(long count) {
        return count == 1 ? "student is" : "students are";
    }
}
