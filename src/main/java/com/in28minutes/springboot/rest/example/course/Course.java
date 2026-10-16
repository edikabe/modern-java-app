package com.in28minutes.springboot.rest.example.course;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Schema(description = "Cours identifié par un code unique")
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

  @Schema(
    description = "Identifiant généré. À omettre lors du POST : toute valeur non nulle " +
      "est refusée. Lors du PUT, l'identifiant du chemin fait foi.",
    example = "1"
  )
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Schema(
    description = "Titre du cours, texte obligatoire et non blanc",
    example = "Introduction à Java",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Column(nullable = false, columnDefinition = "text")
  @JsonDeserialize(using = CourseJsonDeserializers.Text.class)
  private String titre;

  @Schema(
    description = "Code du cours, texte obligatoire, non blanc et unique parmi les cours",
    example = "JAVA-101",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Column(nullable = false, columnDefinition = "text")
  @JsonDeserialize(using = CourseJsonDeserializers.Text.class)
  private String code;

  @Schema(
    description = "Date du cours obligatoire, date valide au format YYYY-MM-DD",
    type = "string",
    format = "date",
    pattern = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$",
    example = "2026-10-16",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  @Column(name = "course_date", nullable = false)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  @JsonDeserialize(using = CourseJsonDeserializers.Date.class)
  private LocalDate date;
}
