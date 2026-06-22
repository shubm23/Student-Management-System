package com.example.sms.service;

import com.example.sms.dto.StudentRequest;
import com.example.sms.dto.StudentResponse;
import com.example.sms.entity.Student;
import com.example.sms.entity.Subject;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public StudentService(StudentRepository studentRepository, SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public StudentResponse addStudent(StudentRequest request) {
        String name = request.getName();
        String email = request.getEmail();
        ensureEmailIsAvailable(email);

        Student student = new Student(name, email);
        Student saved = studentRepository.save(student);
        return StudentResponse.from(saved);
    }

    @Transactional
    public StudentResponse updateStudent(Long studentId, StudentRequest request) {
        Student student = getStudentOrThrow(studentId);
        String name = request.getName();
        String email = request.getEmail();

        boolean emailChanged = !student.getEmail().equalsIgnoreCase(email);
        if (emailChanged) {
            ensureEmailIsAvailable(email);
        }

        student.updateDetails(name, email);
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse assignSubject(Long studentId, Long subjectId) {
        Student student = getStudentOrThrow(studentId);
        Subject subject = getSubjectOrThrow(subjectId);

        student.addSubject(subject);
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudentsWithSubjects() {
        return studentRepository.findAllWithSubjects().stream()
                .map(StudentResponse::from)
                .toList();
    }

    private Student getStudentOrThrow(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("No student found with id: " + studentId));
    }

    private Subject getSubjectOrThrow(Long subjectId) {
        return subjectRepository.findByIdForUpdate(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("No subject found with id: " + subjectId));
    }

    private void ensureEmailIsAvailable(String email) {
        if (studentRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A student with this email already exists: " + email);
        }
    }

}
