package com.in28minutes.springboot.rest.example;

import com.scalar.maven.webmvc.ScalarWebMvcAutoConfiguration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// Springdoc 3.0 filtre l'auto-configuration Scalar ; l'import explicite évite
// le starter Scalar Springdoc, qui active aussi la confiance aux en-têtes Forwarded.
@Configuration(proxyBeanMethods = false)
@Import(ScalarWebMvcAutoConfiguration.class)
@OpenAPIDefinition(
  info = @Info(
    title = "API Étudiants et cours",
    version = "1.0",
    description = "Gestion des étudiants et des cours. Les données H2 sont en mémoire et perdues au redémarrage."
  )
)
public class OpenApiConfiguration {}
