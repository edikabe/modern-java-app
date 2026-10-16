package com.in28minutes.springboot.rest.example.course;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class CourseResource {

  private final CourseRepository courseRepository;

  public CourseResource(CourseRepository courseRepository) {
    this.courseRepository = courseRepository;
  }

  @ExceptionHandler(CourseNotFoundException.class)
  public ProblemDetail handleNotFound(CourseNotFoundException exception) {
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.NOT_FOUND,
      exception.getMessage()
    );
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
  })
  public ProblemDetail handleBadRequest(Exception exception) {
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.BAD_REQUEST,
      "Requête invalide : titre et code doivent être des textes non blancs, " +
        "date une date valide YYYY-MM-DD et l'identifiant un entier."
    );
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
          .matches("(?:.*\\.)?uk_course_code(?:_index_[a-z0-9]+)?")
      ) {
        return ProblemDetail.forStatusAndDetail(
          HttpStatus.CONFLICT,
          "Un cours possède déjà ce code."
        );
      }
    }
    throw exception;
  }

  @GetMapping("/courses")
  public List<Course> retrieveAllCourses() {
    return courseRepository.findAll();
  }

  @GetMapping("/courses/{id}")
  public Course retrieveCourse(@PathVariable long id) {
    return courseRepository
      .findById(id)
      .orElseThrow(() -> new CourseNotFoundException(id));
  }

  @PostMapping("/courses")
  public ResponseEntity<?> createCourse(@Valid @RequestBody Course course) {
    if (course.getId() != null) {
      return ResponseEntity.badRequest().body(
        ProblemDetail.forStatusAndDetail(
          HttpStatus.BAD_REQUEST,
          "L'identifiant est généré et ne doit pas être fourni à la création."
        )
      );
    }

    var savedCourse = courseRepository.saveAndFlush(course);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
      .path("/{id}")
      .buildAndExpand(savedCourse.getId())
      .toUri();
    return ResponseEntity.created(location).build();
  }

  @PutMapping("/courses/{id}")
  public ResponseEntity<@NonNull Void> updateCourse(
    @Valid @RequestBody Course course,
    @PathVariable long id
  ) {
    retrieveCourse(id);
    course.setId(id);
    courseRepository.saveAndFlush(course);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/courses/{id}")
  public ResponseEntity<@NonNull Void> deleteCourse(@PathVariable long id) {
    Course course = retrieveCourse(id);
    courseRepository.delete(course);
    return ResponseEntity.noContent().build();
  }
}
