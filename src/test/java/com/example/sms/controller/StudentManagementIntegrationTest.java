package com.example.sms.controller;

import com.example.sms.entity.Student;
import com.example.sms.entity.Subject;
import com.example.sms.repository.StudentRepository;
import com.example.sms.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @BeforeEach
    void cleanDatabase() {
        studentRepository.deleteAll();
        subjectRepository.deleteAll();
    }

    @Test
    void studentEndpoints_createUpdateAndValidateRequests() throws Exception {
        String responseBody = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Alice  ",
                                  "email": "  ALICE@Example.com  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long studentId = studentRepository.findAll().get(0).getId();
        assertThat(responseBody).contains("\"id\":" + studentId);

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice Smith",
                                  "email": "alice.smith@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void duplicateStudentEmail_isRejectedIgnoringCaseAndWhitespace() throws Exception {
        studentRepository.save(new Student("Alice", "alice@example.com"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Another Alice",
                                  "email": "  ALICE@EXAMPLE.COM  "
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "A student with this email already exists: alice@example.com"));

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    @Test
    void updateStudent_toAnotherStudentsEmail_isRejectedWithoutChangingStudent() throws Exception {
        Student alice = studentRepository.save(new Student("Alice", "alice@example.com"));
        studentRepository.save(new Student("Bob", "bob@example.com"));

        mockMvc.perform(put("/api/students/{id}", alice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Changed Alice",
                                  "email": "BOB@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        Student unchanged = studentRepository.findById(alice.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Alice");
        assertThat(unchanged.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void subjectEndpoint_rejectsDuplicateAndBlankNames() throws Exception {
        subjectRepository.save(new Subject("Math"));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  math  "}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(subjectRepository.count()).isEqualTo(1);
    }

    @Test
    void subjectAssignment_persistsJoinAndFetchesStudentsWithSubjects() throws Exception {
        Student student = studentRepository.save(new Student("Alice", "alice@example.com"));
        Subject subject = subjectRepository.save(new Subject("Math"));

        mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                        student.getId(), subject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects[0].id").value(subject.getId()))
                .andExpect(jsonPath("$.subjects[0].name").value("Math"));

        List<Student> students = studentRepository.findAllWithSubjects();
        assertThat(students).hasSize(1);
        assertThat(students.get(0).getSubjects())
                .extracting(Subject::getName)
                .containsExactly("Math");

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(student.getId()))
                .andExpect(jsonPath("$[0].subjects[0].name").value("Math"));
    }

    @Test
    void assigningSameSubjectTwice_isIdempotent() throws Exception {
        Student student = studentRepository.save(new Student("Alice", "alice@example.com"));
        Subject subject = subjectRepository.save(new Subject("Math"));

        mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                        student.getId(), subject.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                        student.getId(), subject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(1));

        Student persisted = studentRepository.findAllWithSubjects().get(0);
        assertThat(persisted.getSubjects()).hasSize(1);
        assertThat(studentRepository.countStudentsBySubjectId(subject.getId())).isEqualTo(1);
    }

    @Test
    void assignmentAndMutationEndpoints_returnNotFoundForMissingResources() throws Exception {
        Student student = studentRepository.save(new Student("Alice", "alice@example.com"));
        Subject subject = subjectRepository.save(new Subject("Math"));

        mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                        999999L, subject.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                        student.getId(), 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(put("/api/students/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Missing",
                                  "email": "missing@example.com"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(delete("/api/subjects/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedJsonAndMissingFields_returnBadRequest() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains invalid JSON."));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getStudents_whenDatabaseIsEmpty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteSubject_blocksAssignedSubjectAndDeletesUnassignedSubject() throws Exception {
        Student student = studentRepository.save(new Student("Alice", "alice@example.com"));
        Subject assigned = subjectRepository.save(new Subject("Math"));
        Subject unassigned = subjectRepository.save(new Subject("Physics"));

        mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                        student.getId(), assigned.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/subjects/{id}", assigned.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(subjectRepository.existsById(assigned.getId())).isTrue();

        mockMvc.perform(delete("/api/subjects/{id}", unassigned.getId()))
                .andExpect(status().isNoContent());

        assertThat(subjectRepository.existsById(unassigned.getId())).isFalse();
    }

    @Test
    void deletingAndAssigningSameSubject_concurrentlyCannotBothSucceed() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= 10; round++) {
                Student student = studentRepository.save(
                        new Student("Student " + round, "student" + round + "@example.com"));
                Subject subject = subjectRepository.save(new Subject("Subject " + round));
                CountDownLatch start = new CountDownLatch(1);

                Future<Integer> assignment = executor.submit(() -> {
                    start.await();
                    return mockMvc.perform(post("/api/students/{studentId}/subjects/{subjectId}",
                                    student.getId(), subject.getId()))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                });
                Future<Integer> deletion = executor.submit(() -> {
                    start.await();
                    return mockMvc.perform(delete("/api/subjects/{id}", subject.getId()))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                });

                start.countDown();
                int assignmentStatus = assignment.get();
                int deletionStatus = deletion.get();

                assertThat(assignmentStatus == 200 && deletionStatus == 204)
                        .as("assignment and deletion must not both report success")
                        .isFalse();

                if (assignmentStatus == 200) {
                    assertThat(deletionStatus).isEqualTo(409);
                    assertThat(subjectRepository.existsById(subject.getId())).isTrue();
                    assertThat(studentRepository.countStudentsBySubjectId(subject.getId())).isEqualTo(1);
                } else {
                    assertThat(assignmentStatus).isEqualTo(404);
                    assertThat(deletionStatus).isEqualTo(204);
                    assertThat(subjectRepository.existsById(subject.getId())).isFalse();
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
