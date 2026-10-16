package com.in28minutes.springboot.rest.example.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Étudiants", description = "Gestion des étudiants")
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

  @Operation(summary = "Lister les étudiants")
  @ApiResponse(
    responseCode = "200",
    description = "Liste des étudiants",
    useReturnTypeSchema = true
  )
  @GetMapping("/students")
  public List<Student> retrieveAllStudents() {
    return studentRepository.findAll();
  }

  @Operation(summary = "Consulter un étudiant")
  @ApiResponse(
    responseCode = "200",
    description = "Étudiant trouvé",
    useReturnTypeSchema = true
  )
  @GetMapping("/students/{id}")
  public Student retrieveStudent(@PathVariable long id) {
    Optional<Student> student = studentRepository.findById(id);

    if (student.isEmpty()) throw new StudentNotFoundException("id-" + id);

    return student.get();
  }

  @Operation(summary = "Supprimer un étudiant")
  @ApiResponse(
    responseCode = "200",
    description = "Suppression effectuée, sans corps de réponse, " +
      "y compris si l'étudiant est absent",
    content = @Content
  )
  @DeleteMapping("/students/{id}")
  public void deleteStudent(@PathVariable long id) {
    studentRepository.deleteById(id);
  }

  @Operation(summary = "Créer un étudiant")
  @ApiResponse(
    responseCode = "201",
    description = "Étudiant créé, sans corps de réponse",
    headers = @Header(
      name = "Location",
      description = "URI de l'étudiant créé",
      schema = @Schema(type = "string", format = "uri")
    ),
    content = @Content
  )
  @ApiResponse(
    responseCode = "409",
    description = "Numéro de passeport déjà utilisé par un autre étudiant",
    content = @Content(
      mediaType = "application/problem+json",
      schema = @Schema(implementation = ProblemDetail.class)
    )
  )
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

  @Operation(
    summary = "Remplacer un étudiant",
    description = "Remplace le nom et le numéro de passeport. " +
      "L'identifiant du chemin fait foi, quel que soit celui du corps."
  )
  @ApiResponse(
    responseCode = "204",
    description = "Étudiant remplacé, sans corps de réponse",
    content = @Content
  )
  @ApiResponse(
    responseCode = "404",
    description = "Étudiant introuvable, sans corps de réponse",
    content = @Content
  )
  @ApiResponse(
    responseCode = "409",
    description = "Numéro de passeport déjà utilisé par un autre étudiant",
    content = @Content(
      mediaType = "application/problem+json",
      schema = @Schema(implementation = ProblemDetail.class)
    )
  )
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
