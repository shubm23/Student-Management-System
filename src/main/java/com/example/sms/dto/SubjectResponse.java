package com.example.sms.dto;

import com.example.sms.entity.Subject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {

    private Long id;
    private String name;

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName());
    }
}
