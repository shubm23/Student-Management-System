# Student Management System

This is a small Spring Boot API for managing students and the subjects assigned
to them. I kept the code split into controllers, services, repositories, DTOs,
and entities so the flow stays easy to follow.

## Tech stack
- Java 17
- Spring Boot 3.3.2
- Spring Web
- Spring Data JPA / Hibernate
- MySQL for local runs
- H2 database for tests
- JUnit 5 + Mockito + AssertJ

## Data model
- `student`: stores the student's id, name, and unique email
- `subject`: stores the subject id and unique subject name
- `student_subject`: join table used for student-subject assignments

## APIs

| Method | Path | What it does |
| --- | --- | --- |
| `POST` | `/api/students` | Creates a student |
| `PUT` | `/api/students/{id}` | Updates a student |
| `GET` | `/api/students` | Lists students with their subjects |
| `POST` | `/api/students/{studentId}/subjects/{subjectId}` | Assigns a subject to a student |
| `POST` | `/api/subjects` | Creates a subject |
| `DELETE` | `/api/subjects/{id}` | Deletes a subject if no student is using it |

### Sample requests
```bash
# create a student
curl -X POST localhost:8080/api/students \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com"}'

# create a subject
curl -X POST localhost:8080/api/subjects \
  -H 'Content-Type: application/json' \
  -d '{"name":"Math"}'

# assign subject 1 to student 1
curl -X POST localhost:8080/api/students/1/subjects/1

# list students
curl localhost:8080/api/students

# delete a subject
curl -X DELETE localhost:8080/api/subjects/1
```


## Run

Create the MySQL database first:
```sql
CREATE DATABASE student_management;
```

The app uses these defaults:
```properties
DB_URL=jdbc:mysql://localhost:3306/student_management
DB_USERNAME=root
DB_PASSWORD=
```

You can override these values using environment variables in your IDE's run
configuration.

Hibernate creates/updates the required tables automatically.

## Concurrency handling

Student and subject records use optimistic locking. If two requests try to
change the same record concurrently, or a database constraint is hit because
of a concurrent request, the API returns `409 Conflict` instead of exposing an
internal server error.

Student names and subject names are trimmed before saving. Emails are trimmed
and converted to lowercase. Names are limited to 100 characters and emails to
255 characters.

The unit tests focus on the service layer: creating students and subjects,
updating students, assigning subjects, and blocking a subject delete when it is
already assigned.
