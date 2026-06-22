package com.example.sms.service;

import com.example.sms.dto.SubjectRequest;
import com.example.sms.dto.SubjectResponse;
import com.example.sms.entity.Subject;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.exception.SubjectInUseException;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    void addSubject_savesAndReturnsResponse() {
        SubjectRequest request = new SubjectRequest();
        request.setName("Physics");

        when(subjectRepository.existsByNameIgnoreCase("Physics")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        SubjectResponse response = subjectService.addSubject(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Physics");
    }

    @Test
    void addSubject_throwsWhenNameExists() {
        SubjectRequest request = new SubjectRequest();
        request.setName("Physics");

        when(subjectRepository.existsByNameIgnoreCase("Physics")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.addSubject(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void deleteSubject_deletesWhenNotAssigned() {
        Subject subject = new Subject("Physics");
        subject.setId(10L);

        when(subjectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(subject));
        when(studentRepository.countStudentsBySubjectId(10L)).thenReturn(0L);

        subjectService.deleteSubject(10L);

        verify(subjectRepository).delete(subject);
    }

    @Test
    void deleteSubject_throwsWhenAssignedToStudents() {
        Subject subject = new Subject("Physics");
        subject.setId(10L);

        when(subjectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(subject));
        when(studentRepository.countStudentsBySubjectId(10L)).thenReturn(3L);

        assertThatThrownBy(() -> subjectService.deleteSubject(10L))
                .isInstanceOf(SubjectInUseException.class)
                .hasMessageContaining("3");

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteSubject_throwsWhenNotFound() {
        when(subjectRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.deleteSubject(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addSubject_trimsName() {
        SubjectRequest request = new SubjectRequest();
        request.setName("  Physics  ");

        when(subjectRepository.existsByNameIgnoreCase("Physics")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(i -> i.getArgument(0));

        SubjectResponse response = subjectService.addSubject(request);

        assertThat(response.getName()).isEqualTo("Physics");
    }
}
