package com.example.sms.dto;

import com.example.sms.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String name;
    private String email;
    private List<SubjectResponse> subjects;

    public static StudentResponse from(Student student) {
        List<SubjectResponse> subjectResponses = student.getSubjects().stream()
                .map(SubjectResponse::from)
                .toList();
        return new StudentResponse(student.getId(), student.getName(), student.getEmail(), subjectResponses);
    }
}
