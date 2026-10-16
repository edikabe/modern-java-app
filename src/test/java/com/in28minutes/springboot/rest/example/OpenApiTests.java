package com.in28minutes.springboot.rest.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = "spring.datasource.url=jdbc:h2:mem:openapi-tests-${random.uuid}"
)
class OpenApiTests {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @LocalServerPort
  private int port;

  @Test
  void specificationDocumentsOnlyBusinessEndpoints() throws Exception {
    JsonNode document = specification();
    assertTrue(document.path("openapi").asString().startsWith("3."));
    assertEquals(
      "API Étudiants et cours",
      document.at("/info/title").asString()
    );
    JsonNode paths = document.path("paths");
    assertEquals(
      Set.of("/students", "/students/{id}", "/courses", "/courses/{id}"),
      paths.propertyNames()
    );
    for (String resource : new String[] { "/students", "/courses" }) {
      assertTrue(paths.path(resource).has("get"));
      assertTrue(paths.path(resource).has("post"));
      for (String method : new String[] { "get", "put", "delete" }) {
        assertTrue(paths.path(resource + "/{id}").has(method));
      }
    }
  }

  @Test
  void specificationDescribesEmptyResponsesAndConflicts() throws Exception {
    JsonNode paths = specification().path("paths");
    for (String resource : new String[] { "/students", "/courses" }) {
      JsonNode creation = paths.path(resource).path("post").path("responses");
      assertTrue(creation.has("201"));
      assertFalse(creation.has("200"));
      assertFalse(creation.path("201").has("content"));
      assertTrue(creation.path("201").path("headers").has("Location"));
      assertProblem(creation, "409");

      JsonNode update = paths
        .path(resource + "/{id}")
        .path("put")
        .path("responses");
      assertTrue(update.has("204"));
      assertFalse(update.path("204").has("content"));
      assertTrue(update.has("404"));
      assertProblem(update, "409");
    }
    assertProblem(paths.path("/courses").path("post").path("responses"), "400");
    assertProblem(
      paths.path("/courses/{id}").path("get").path("responses"),
      "404"
    );
    assertTrue(
      paths.path("/courses/{id}").path("delete").path("responses").has("204")
    );
    assertTrue(
      paths.path("/students/{id}").path("delete").path("responses").has("200")
    );
  }

  @Test
  void specificationIncludesModelsAndCourseConstraints() throws Exception {
    JsonNode schemas = specification().path("components").path("schemas");
    assertTrue(
      schemas.path("Student").path("properties").has("passportNumber")
    );
    JsonNode course = schemas.path("Course");
    for (String field : new String[] { "titre", "code", "date" }) {
      assertTrue(
        course
          .path("required")
          .toString()
          .contains("\"" + field + "\"")
      );
    }
    assertEquals("date", course.at("/properties/date/format").asString());
  }

  @Test
  void yamlSpecificationIsAvailable() throws Exception {
    HttpResponse<String> response = get("/v3/api-docs.yaml");
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("openapi: 3."));
    assertTrue(response.body().contains("/courses:"));
    assertTrue(response.body().contains("/students:"));
  }

  @Test
  void scalarPageReferencesSpecificationAndServesLocalJavascript()
    throws Exception {
    HttpResponse<String> response = get("/docs");
    assertEquals(200, response.statusCode());
    assertTrue(
      response
        .headers()
        .firstValue("Content-Type")
        .orElse("")
        .contains("text/html")
    );
    assertTrue(response.body().contains("./v3/api-docs"));
    assertTrue(response.body().contains("Scalar.createApiReference"));
    var script = java.util.regex.Pattern.compile(
      "<script[^>]+src=\"([^\"]+)\""
    ).matcher(response.body());
    assertTrue(script.find(), "Scalar doit charger son bundle JavaScript");
    URI scriptUri = URI.create("http://localhost:" + port + "/docs").resolve(
      script.group(1)
    );
    assertEquals("localhost", scriptUri.getHost());
    assertEquals(port, scriptUri.getPort());
    HttpResponse<String> javascript = get(scriptUri.getPath());
    assertEquals(200, javascript.statusCode());
    assertTrue(
      javascript
        .headers()
        .firstValue("Content-Type")
        .orElse("")
        .contains("javascript")
    );
    assertFalse(javascript.body().isBlank());
    assertEquals(404, get("/swagger-ui/index.html").statusCode());
  }

  private JsonNode specification() throws Exception {
    HttpResponse<String> response = get("/v3/api-docs");
    assertEquals(200, response.statusCode());
    return JSON.readTree(response.body());
  }

  private void assertProblem(JsonNode responses, String status) {
    assertEquals(
      "#/components/schemas/ProblemDetail",
      responses
        .path(status)
        .path("content")
        .path("application/problem+json")
        .path("schema")
        .path("$ref")
        .asString()
    );
  }

  private HttpResponse<String> get(String path) throws Exception {
    try (
      HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    ) {
      return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
          .timeout(Duration.ofSeconds(30))
          .GET()
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );
    }
  }
}
