package com.in28minutes.springboot.rest.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.in28minutes.springboot.rest.example.course.Course;
import com.in28minutes.springboot.rest.example.course.CourseRepository;
import com.in28minutes.springboot.rest.example.student.Student;
import com.in28minutes.springboot.rest.example.student.StudentRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// H2 vérifie la configuration du profil, pas la compatibilité PostgreSQL réelle.
@Tag("integration")
@ActiveProfiles("postgres")
@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:postgres-profile-tests-${random.uuid};MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
  }
)
@Transactional
class PostgresProfileTests {

  @Autowired
  private Environment environment;

  @Autowired
  private StudentRepository studentRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Test
  void profileDisablesH2ConsoleAndSqlScriptsAndUsesHibernateUpdate() {
    assertEquals(
      Boolean.FALSE,
      environment.getProperty("spring.h2.console.enabled", Boolean.class)
    );
    assertEquals("never", environment.getProperty("spring.sql.init.mode"));
    assertEquals(
      "update",
      environment.getProperty("spring.jpa.hibernate.ddl-auto")
    );
    assertEquals(0L, studentRepository.count());
    assertEquals(0L, courseRepository.count());
  }

  @Test
  void generatedStudentSchemaSupportsGeneratedIdsAndUniquePassports() {
    Student student = new Student();
    student.setName("Test Student");
    student.setPassportNumber("POSTGRES-PASSPORT");
    assertNull(student.getId());

    Student saved = studentRepository.saveAndFlush(student);

    assertNotNull(saved.getId());
    assertTrue(saved.getId() > 0);
    assertEquals(1L, studentRepository.count());

    Student duplicate = new Student();
    duplicate.setName("Another Student");
    duplicate.setPassportNumber(student.getPassportNumber());

    assertThrows(DataIntegrityViolationException.class, () ->
      studentRepository.saveAndFlush(duplicate)
    );
  }

  @Test
  void generatedCourseSchemaSupportsGeneratedIdsAndUniqueCodes() {
    Course course = new Course();
    course.setTitre("Introduction à Java");
    course.setCode("POSTGRES-JAVA");
    course.setDate(LocalDate.of(2026, 10, 16));
    assertNull(course.getId());

    Course saved = courseRepository.saveAndFlush(course);

    assertNotNull(saved.getId());
    assertTrue(saved.getId() > 0);
    assertEquals(1L, courseRepository.count());

    Course duplicate = new Course();
    duplicate.setTitre("Autre cours");
    duplicate.setCode(course.getCode());
    duplicate.setDate(LocalDate.of(2026, 10, 17));

    assertThrows(DataIntegrityViolationException.class, () ->
      courseRepository.saveAndFlush(duplicate)
    );
  }
}
