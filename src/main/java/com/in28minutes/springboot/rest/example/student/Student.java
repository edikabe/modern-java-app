package com.in28minutes.springboot.rest.example.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Étudiant avec un numéro de passeport unique")
@Data
@NoArgsConstructor
@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_student_passport",
    columnNames = "passport_number"
  )
)
public class Student {

  @Schema(
    description = "Identifiant généré, à omettre pour créer un nouvel étudiant. " +
      "Lors du PUT, l'identifiant du chemin fait foi.",
    example = "1"
  )
  @Id
  @GeneratedValue
  private Long id;

  @Schema(description = "Nom de l'étudiant", example = "Camille Martin")
  private String name;

  @Schema(
    description = "Numéro de passeport unique parmi les étudiants lorsqu'il est renseigné",
    example = "AB1234567"
  )
  @Column(name = "passport_number")
  private String passportNumber;
}
