package com.example.sms.repository;

import com.example.sms.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "subjects")
    @Query("SELECT DISTINCT s FROM Student s")
    List<Student> findAllWithSubjects();

    @Query("SELECT COUNT(s) FROM Student s JOIN s.subjects sub WHERE sub.id = :subjectId")
    long countStudentsBySubjectId(@Param("subjectId") Long subjectId);
}
