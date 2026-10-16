package com.in28minutes.springboot.rest.example.course;

public class CourseNotFoundException extends RuntimeException {

  public CourseNotFoundException(long id) {
    super("Aucun cours avec l'identifiant " + id + ".");
  }
}
