package com.in28minutes.springboot.rest.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Tag("integration")
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = "spring.datasource.url=jdbc:h2:mem:course-tests-${random.uuid}"
)
@Execution(ExecutionMode.SAME_THREAD)
class CourseTests {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @LocalServerPort
  private int port;

  @Test
  void creationReturnsLocationAndEmptyBodyAndItemCanBeRetrieved()
    throws Exception {
    ObjectNode body = validCourse();
    String path = createCourse(body);

    assertCourse(readCourse(path), body, idFromPath(path));
  }

  @Test
  void collectionContainsCreatedCoursesRegardlessOfOrder() throws Exception {
    ObjectNode first = validCourse();
    ObjectNode second = validCourse();
    String firstPath = createCourse(first);
    String secondPath = createCourse(second);

    Map<Long, JsonNode> courses = coursesById();

    assertCourse(
      courses.get(idFromPath(firstPath)),
      first,
      idFromPath(firstPath)
    );
    assertCourse(
      courses.get(idFromPath(secondPath)),
      second,
      idFromPath(secondPath)
    );
  }

  @Test
  void updateReplacesEveryFieldAndReturnsNoContent() throws Exception {
    String path = createCourse(validCourse());
    ObjectNode replacement = validCourse();
    replacement.put("date", "2027-12-31");

    assertNoContent(request("PUT", path, replacement));

    assertCourse(readCourse(path), replacement, idFromPath(path));
  }

  @Test
  void deletionReturnsNoContentAndRemovesCourse() throws Exception {
    String path = createCourse(validCourse());

    assertNoContent(request("DELETE", path, null));

    assertProblem(request("GET", path, null), 404, path);
    assertFalse(coursesById().containsKey(idFromPath(path)));
    assertProblem(request("DELETE", path, null), 404, path);
  }

  @Test
  void duplicateCreationReturnsConflictAndPreservesState() throws Exception {
    ObjectNode original = validCourse();
    String path = createCourse(original);
    Map<Long, JsonNode> before = coursesById();
    ObjectNode duplicate = validCourse();
    duplicate.set("code", original.get("code"));
    duplicate.put("date", "2027-12-31");

    assertProblem(request("POST", "/courses", duplicate), 409, "/courses");

    assertCourse(readCourse(path), original, idFromPath(path));
    assertEquals(before, coursesById());
  }

  @Test
  void duplicateUpdateReturnsConflictAndPreservesBothCourses()
    throws Exception {
    ObjectNode first = validCourse();
    ObjectNode second = validCourse();
    String firstPath = createCourse(first);
    String secondPath = createCourse(second);
    Map<Long, JsonNode> before = coursesById();
    ObjectNode replacement = validCourse();
    replacement.set("code", first.get("code"));
    replacement.put("date", "2027-12-31");

    assertProblem(request("PUT", secondPath, replacement), 409, secondPath);

    assertCourse(readCourse(firstPath), first, idFromPath(firstPath));
    assertCourse(readCourse(secondPath), second, idFromPath(secondPath));
    assertEquals(before, coursesById());
  }

  @Test
  void updateMayKeepItsOwnCode() throws Exception {
    ObjectNode original = validCourse();
    String path = createCourse(original);
    ObjectNode replacement = validCourse();
    replacement.set("code", original.get("code"));
    replacement.put("date", "2027-12-31");

    assertNoContent(request("PUT", path, replacement));

    assertCourse(readCourse(path), replacement, idFromPath(path));
  }

  @Test
  void creationWithExistingIdCannotOverwriteCourse() throws Exception {
    ObjectNode original = validCourse();
    String path = createCourse(original);
    Map<Long, JsonNode> before = coursesById();
    ObjectNode replacement = validCourse();
    replacement.put("id", idFromPath(path));
    replacement.put("date", "2027-12-31");

    assertProblem(request("POST", "/courses", replacement), 400, "/courses");

    assertCourse(readCourse(path), original, idFromPath(path));
    assertEquals(before, coursesById());
  }

  @Test
  void creationRejectsEvenAnUnusedNonNullId() throws Exception {
    ObjectNode body = validCourse();
    body.put("id", Long.MAX_VALUE);
    Map<Long, JsonNode> before = coursesById();
    assertFalse(before.containsKey(Long.MAX_VALUE));

    assertProblem(request("POST", "/courses", body), 400, "/courses");

    assertEquals(before, coursesById());
  }

  @Test
  void creationAcceptsExplicitNullId() throws Exception {
    ObjectNode body = validCourse();
    body.putNull("id");

    String path = createCourse(body);

    assertCourse(readCourse(path), body, idFromPath(path));
  }

  @Test
  void updateUsesPathIdInsteadOfDifferentBodyId() throws Exception {
    String targetPath = createCourse(validCourse());
    ObjectNode other = validCourse();
    String otherPath = createCourse(other);
    Map<Long, JsonNode> expected = coursesById();
    ObjectNode replacement = validCourse();
    replacement.put("id", idFromPath(otherPath));
    replacement.put("date", "2027-12-31");

    assertNoContent(request("PUT", targetPath, replacement));

    assertCourse(readCourse(targetPath), replacement, idFromPath(targetPath));
    assertCourse(readCourse(otherPath), other, idFromPath(otherPath));
    ObjectNode updated = replacement.deepCopy();
    updated.set("id", expected.get(idFromPath(targetPath)).get("id"));
    expected.put(idFromPath(targetPath), updated);
    assertEquals(expected, coursesById());
  }

  @Test
  void validLeapDayIsAcceptedAndReturnedWithoutTime() throws Exception {
    ObjectNode body = validCourse();
    body.put("date", "2028-02-29");
    String path = createCourse(body);

    assertCourse(readCourse(path), body, idFromPath(path));
  }

  @ParameterizedTest(name = "{0} on an absent course returns 404")
  @ValueSource(strings = { "GET", "PUT", "DELETE" })
  void absentCourseReturnsProblemDetail(String method) throws Exception {
    String path = "/courses/" + Long.MAX_VALUE;
    Map<Long, JsonNode> before = coursesById();
    assertFalse(before.containsKey(Long.MAX_VALUE));

    assertProblem(
      request(method, path, "PUT".equals(method) ? validCourse() : null),
      404,
      path
    );

    assertEquals(before, coursesById());
  }

  @ParameterizedTest(
    name = "POST rejects {0} = {1} (null argument means omitted)"
  )
  @MethodSource("invalidFields")
  void invalidCreationReturnsBadRequestAndPreservesState(
    String field,
    String jsonValue
  ) throws Exception {
    ObjectNode body = invalidCourse(field, jsonValue);
    Map<Long, JsonNode> before = coursesById();

    assertProblem(request("POST", "/courses", body), 400, "/courses");

    assertEquals(before, coursesById());
  }

  @ParameterizedTest(
    name = "PUT rejects {0} = {1} (null argument means omitted)"
  )
  @MethodSource("invalidFields")
  void invalidReplacementReturnsBadRequestAndPreservesState(
    String field,
    String jsonValue
  ) throws Exception {
    ObjectNode original = validCourse();
    String path = createCourse(original);
    ObjectNode replacement = invalidCourse(field, jsonValue);
    Map<Long, JsonNode> before = coursesById();

    assertProblem(request("PUT", path, replacement), 400, path);

    assertCourse(readCourse(path), original, idFromPath(path));
    assertEquals(before, coursesById());
  }

  private static Stream<Arguments> invalidFields() {
    Stream<Arguments> requiredTextAndDate = Stream.of(
      "titre",
      "code",
      "date"
    ).flatMap(field ->
      Stream.<String>of(
        null,
        "null",
        "\"\"",
        "\" \\t\\n \"",
        "123",
        "true",
        "{}",
        "[]"
      ).map(value -> Arguments.of(field, value))
    );
    Stream<Arguments> invalidDates = Stream.of(
      "2026-02-30",
      "2025-02-29",
      "2026-13-01",
      "2026-01-00",
      "2026-04-31",
      "2026-2-01",
      "2026-02-1",
      "01/02/2026",
      "2026-02-01T00:00:00Z",
      " 2026-02-01 "
    ).map(value -> Arguments.of("date", JSON.writeValueAsString(value)));
    return Stream.concat(requiredTextAndDate, invalidDates);
  }

  private ObjectNode validCourse() {
    return JSON.createObjectNode()
      .put("titre", "Cours " + UUID.randomUUID())
      .put("code", UUID.randomUUID().toString())
      .put("date", "2026-10-16");
  }

  private ObjectNode invalidCourse(String field, String jsonValue) {
    ObjectNode body = validCourse();
    if (jsonValue == null) {
      body.remove(field);
    } else {
      body.set(field, JSON.readTree(jsonValue));
    }
    return body;
  }

  private String createCourse(ObjectNode body) throws Exception {
    HttpResponse<String> response = request("POST", "/courses", body);
    assertEquals(201, response.statusCode(), response.body());
    assertEquals("", response.body());
    URI location = URI.create(
      response.headers().firstValue("Location").orElseThrow()
    );
    assertEquals("http", location.getScheme());
    assertEquals("localhost", location.getHost());
    assertEquals(port, location.getPort());
    assertTrue(location.getPath().matches("/courses/[0-9]+"));
    assertTrue(idFromPath(location.getPath()) > 0);
    return location.getPath();
  }

  private JsonNode readCourse(String path) throws Exception {
    HttpResponse<String> response = request("GET", path, null);
    assertEquals(200, response.statusCode(), response.body());
    assertMediaType(response, "application/json");
    JsonNode course = JSON.readTree(response.body());
    assertTrue(course.isObject(), response.body());
    return course;
  }

  private Map<Long, JsonNode> coursesById() throws Exception {
    HttpResponse<String> response = request("GET", "/courses", null);
    assertEquals(200, response.statusCode(), response.body());
    assertMediaType(response, "application/json");
    JsonNode courses = JSON.readTree(response.body());
    assertTrue(courses.isArray(), response.body());
    Map<Long, JsonNode> byId = new HashMap<>();
    for (JsonNode course : courses) {
      assertTrue(course.path("id").isIntegralNumber());
      long id = course.get("id").longValue();
      assertFalse(byId.containsKey(id), "Duplicate course id: " + id);
      byId.put(id, course);
    }
    return byId;
  }

  private void assertCourse(JsonNode actual, ObjectNode body, long id) {
    assertEquals(id, actual.path("id").longValue());
    assertEquals(body.get("titre"), actual.get("titre"));
    assertEquals(body.get("code"), actual.get("code"));
    assertEquals(body.get("date"), actual.get("date"));
    assertEquals(4, actual.size());
    assertFalse(actual.has("title"));
  }

  private void assertNoContent(HttpResponse<String> response) {
    assertEquals(204, response.statusCode(), response.body());
    assertEquals("", response.body());
  }

  private void assertProblem(
    HttpResponse<String> response,
    int status,
    String path
  ) {
    assertEquals(status, response.statusCode(), response.body());
    assertMediaType(response, "application/problem+json");
    JsonNode problem = JSON.readTree(response.body());
    assertTrue(problem.isObject(), response.body());
    assertEquals(JSON.valueToTree(status), problem.get("status"));
    // RFC 9457 : un type absent équivaut à about:blank.
    if (problem.has("type")) {
      assertEquals(JSON.valueToTree("about:blank"), problem.get("type"));
    }
    assertEquals(
      JSON.valueToTree(HttpStatus.valueOf(status).getReasonPhrase()),
      problem.get("title")
    );
    assertEquals(JSON.valueToTree(path), problem.get("instance"));
    assertTrue(problem.hasNonNull("detail"), response.body());
    assertFalse(
      JSON.treeToValue(problem.get("detail"), String.class).isBlank()
    );
  }

  private void assertMediaType(HttpResponse<String> response, String expected) {
    String contentType = response
      .headers()
      .firstValue("Content-Type")
      .orElseThrow();
    assertEquals(expected, contentType.split(";", 2)[0].trim());
  }

  private long idFromPath(String path) {
    return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
  }

  private HttpResponse<String> request(
    String method,
    String path,
    JsonNode body
  ) throws Exception {
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
          .timeout(Duration.ofSeconds(10))
          .header("Content-Type", "application/json")
          .method(
            method,
            body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(
                  JSON.writeValueAsString(body)
                )
          )
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );
    }
  }
}
