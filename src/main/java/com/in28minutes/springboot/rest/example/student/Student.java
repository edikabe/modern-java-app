package com.in28minutes.springboot.rest.example.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Column(name = "passport_number")
  private String passportNumber;
}
