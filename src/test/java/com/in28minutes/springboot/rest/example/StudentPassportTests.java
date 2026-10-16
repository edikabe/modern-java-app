package com.in28minutes.springboot.rest.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.in28minutes.springboot.rest.example.student.Student;
import com.in28minutes.springboot.rest.example.student.StudentRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@Tag("integration")
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = "spring.datasource.url=jdbc:h2:mem:passport-tests"
)
class StudentPassportTests {

  @LocalServerPort
  private int port;

  @Autowired
  private StudentRepository repository;

  @Test
  void duplicateCreationReturnsConflict() throws Exception {
    String passport = UUID.randomUUID().toString();
    assertEquals(201, request("POST", "/students", passport).statusCode());
    long count = repository.count();
    var response = request("POST", "/students", passport);
    assertEquals(409, response.statusCode());
    assertTrue(
      response
        .body()
        .contains("Un étudiant possède déjà ce numéro de passeport.")
    );
    assertEquals(count, repository.count());
  }

  @Test
  void updateCannotReuseAnotherStudentsPassport() throws Exception {
    Student first = createStudent();
    Student second = createStudent();
    String path = "/students/" + second.getId();

    assertEquals(
      409,
      request("PUT", path, first.getPassportNumber()).statusCode()
    );
    assertEquals(
      second.getPassportNumber(),
      repository.findById(second.getId()).orElseThrow().getPassportNumber()
    );
    assertEquals(
      204,
      request("PUT", path, second.getPassportNumber()).statusCode()
    );
    assertEquals(
      204,
      request("PUT", path, UUID.randomUUID().toString()).statusCode()
    );
  }

  private Student createStudent() {
    Student student = new Student();
    student.setName("Test Student");
    student.setPassportNumber(UUID.randomUUID().toString());
    return repository.save(student);
  }

  private HttpResponse<String> request(
    String method,
    String path,
    String passport
  ) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .method(
            method,
            HttpRequest.BodyPublishers.ofString(
              "{\"name\":\"Test Student\",\"passportNumber\":\"" +
                passport +
                "\"}"
            )
          )
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );
    }
  }
}
