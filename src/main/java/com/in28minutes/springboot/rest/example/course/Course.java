package com.in28minutes.springboot.rest.example.course;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@NoArgsConstructor
@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_course_code",
    columnNames = "code"
  )
)
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, columnDefinition = "text")
  @JsonDeserialize(using = CourseJsonDeserializers.Text.class)
  private String titre;

  @NotBlank
  @Column(nullable = false, columnDefinition = "text")
  @JsonDeserialize(using = CourseJsonDeserializers.Text.class)
  private String code;

  @NotNull
  @Column(name = "course_date", nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  @JsonDeserialize(using = CourseJsonDeserializers.Date.class)
  private LocalDate date;
}
