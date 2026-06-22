package com.example.sms.service;

import com.example.sms.dto.StudentRequest;
import com.example.sms.dto.StudentResponse;
import com.example.sms.entity.Student;
import com.example.sms.entity.Subject;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void addStudent_savesAndReturnsResponse() {
        StudentRequest request = new StudentRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");

        when(studentRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        StudentResponse response = studentService.addStudent(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void addStudent_throwsWhenEmailExists() {
        StudentRequest request = new StudentRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");

        when(studentRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.addStudent(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudent_updatesNameAndEmail() {
        Student existing = new Student("Old", "old@example.com");
        existing.setId(5L);

        StudentRequest request = new StudentRequest();
        request.setName("New");
        request.setEmail("new@example.com");

        when(studentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse response = studentService.updateStudent(5L, request);

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateStudent_throwsWhenNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        StudentRequest request = new StudentRequest();
        request.setName("X");
        request.setEmail("x@example.com");

        assertThatThrownBy(() -> studentService.updateStudent(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignSubject_addsSubjectToStudent() {
        Student student = new Student("Alice", "alice@example.com");
        student.setId(1L);
        Subject subject = new Subject("Math");
        subject.setId(2L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(subjectRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(subject));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse response = studentService.assignSubject(1L, 2L);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getSubjects()).contains(subject);
        assertThat(response.getSubjects()).extracting("name").contains("Math");
    }

    @Test
    void assignSubject_throwsWhenSubjectNotFound() {
        Student student = new Student("Alice", "alice@example.com");
        student.setId(1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(subjectRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.assignSubject(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllStudentsWithSubjects_mapsSubjects() {
        Student student = new Student("Alice", "alice@example.com");
        student.setId(1L);
        student.addSubject(new Subject("Math"));

        when(studentRepository.findAllWithSubjects()).thenReturn(List.of(student));

        List<StudentResponse> result = studentService.getAllStudentsWithSubjects();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubjects()).extracting("name").contains("Math");
    }

    @Test
    void addStudent_trimsNameAndNormalizesEmail() {
        StudentRequest request = new StudentRequest();
        request.setName("  Alice  ");
        request.setEmail("  ALICE@Example.COM  ");

        when(studentRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse response = studentService.addStudent(request);

        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }
}
