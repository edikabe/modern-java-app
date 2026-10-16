package com.in28minutes.springboot.rest.example.student;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class StudentResource {

  private final StudentRepository studentRepository;

  public StudentResource(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(
    DataIntegrityViolationException exception
  ) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (
        cause instanceof ConstraintViolationException violation &&
        violation.getConstraintName() != null &&
        violation
          .getConstraintName()
          .toLowerCase(Locale.ROOT)
          .contains("uk_student_passport")
      ) {
        return ProblemDetail.forStatusAndDetail(
          HttpStatus.CONFLICT,
          "Un étudiant possède déjà ce numéro de passeport."
        );
      }
    }
    throw exception;
  }

  @GetMapping("/students")
  public List<Student> retrieveAllStudents() {
    return studentRepository.findAll();
  }

  @GetMapping("/students/{id}")
  public Student retrieveStudent(@PathVariable long id) {
    Optional<Student> student = studentRepository.findById(id);

    if (student.isEmpty()) throw new StudentNotFoundException("id-" + id);

    return student.get();
  }

  @DeleteMapping("/students/{id}")
  public void deleteStudent(@PathVariable long id) {
    studentRepository.deleteById(id);
  }

  @PostMapping("/students")
  public ResponseEntity<@NonNull Student> createStudent(
    @RequestBody Student student
  ) {
    var savedStudent = studentRepository.save(student);

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
      .path("/{id}")
      .buildAndExpand(savedStudent.getId())
      .toUri();

    return ResponseEntity.created(location).build();
  }

  @PutMapping("/students/{id}")
  public ResponseEntity<@NonNull Student> updateStudent(
    @RequestBody Student student,
    @PathVariable long id
  ) {
    Optional<Student> studentDetails = studentRepository.findById(id);

    if (studentDetails.isEmpty()) return ResponseEntity.notFound().build();

    student.setId(id);
    studentRepository.save(student);
    return ResponseEntity.noContent().build();
  }
}
