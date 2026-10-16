package com.in28minutes.springboot.rest.example.course;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<@NonNull Course, @NonNull Long> {

}
