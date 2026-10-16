# Modern Java App

J'ai chopé un repo exemple de projet Spring sur lequel j'ai mis en place toute une chaine de tooling moderne.
Peu importe que ce soit du Java ou autre...

## Instructions pour les agents IA

[AGENTS.md](AGENTS.md) contient le socle chargé dans le contexte : conventions, invariants métier, garde-fous et validation essentielle. Les consignes spécialisées sont lues uniquement selon la tâche :

- [Build, tests, tâches mise, seed et Zed](.agents/references/build.md)
- [Docker, Colima et PostgreSQL](.agents/references/docker.md)
- [GitHub Actions](.agents/references/ci.md)
- [Contrat REST, OpenAPI et Scalar](.agents/references/openapi.md)

Les skills de `.agents/skills/` décrivent les procédures réutilisables et sont chargés à la demande. Lors d’une évolution, mettre à jour la référence concernée ; réserver `AGENTS.md` aux règles transversales et aux renvois conditionnels pour éviter de gonfler le contexte de chaque conversation.

## Configuration Java dans Zed

`.zed/settings.json` active la synchronisation automatique de la configuration Maven par JDT LS (`java.configuration.updateBuildConfiguration = automatic`). Après modification de ces réglages, ouvrir un fichier Java et lancer `editor: restart language server` depuis la palette de commandes de Zed.

Si l’avertissement « Project configuration is not up-to-date with pom.xml, requires an update. » persiste, utiliser l’action de code proposée sur l’avertissement dans `pom.xml` pour mettre à jour la configuration du projet.

## Exécuter les tests

Depuis la racine du projet, avec les outils Java et Maven gérés par `mise` :

| Commande                    | Action                                      |
| --------------------------- | ------------------------------------------- |
| `mise run test`             | Exécuter tous les tests                     |
| `mise run test:unit`        | Exécuter uniquement les tests unitaires     |
| `mise run test:integration` | Exécuter uniquement les tests d’intégration |

Les tests d’intégration portent l’annotation JUnit `@Tag("integration")` au niveau de la classe. Les tests unitaires ne portent pas ce tag. Ajouter ce tag à toute nouvelle classe de tests démarrant un contexte Spring ou utilisant une base de données ou un serveur HTTP.

Les cinq classes actuelles (`SpringBoot2RestServiceApplicationTests`, `StudentPassportTests`, `CourseTests`, `OpenApiTests` et `PostgresProfileTests`) sont des tests d’intégration : `test:unit` n’exécute donc aucun test pour le moment. Les filtres sélectionnent les tests à exécuter, mais Maven compile toujours l’ensemble des sources de test.

`mise run build`, `mise run rebuild` et `mise run verify` continuent d’exécuter tous les tests par défaut. Pour cibler une classe : `mise exec -- mvn -Dtest=CourseTests test`.

## Intégration continue GitHub Actions

Le fichier `.github/workflows/ci.yml` déclenche le workflow **Java CI** à chaque push sur **toutes les branches**, y compris la branche principale. Les tags et les événements de pull request ne déclenchent pas d’exécution supplémentaire.

Le job `verify` utilise un runner GitHub `ubuntu-24.04`, Java 25 (Temurin) et Maven préinstallé sur le runner pour exécuter `mvn -B -ntp verify`, l’équivalent CI de `mise run verify` : compilation, tous les tests (unitaires et intégration) et construction du JAR Spring Boot. Un échec des tests ou du build fait échouer le job.

- Les dépendances Maven sont mises en cache pour accélérer les exécutions suivantes.
- Les rapports Surefire, dont les XML JUnit, sont téléchargeables dans l’artefact `test-reports`, même en cas d’échec des tests.
- Après un build réussi, le JAR `target/*.jar` est téléchargeable dans l’artefact `application-jar`.
- Les artefacts sont conservés pendant sept jours et accessibles depuis l’exécution du workflow dans l’onglet **Actions**.
- Aucun build d’image Docker, aucune publication ni aucun déploiement n’est effectué.

GitHub Actions doit être activé sur le dépôt et les runners hébergés GitHub autorisés. Aucun secret applicatif ni service de base de données externe n’est nécessaire : les tests utilisent H2 en mémoire. Les permissions du workflow sont limitées à la lecture du contenu du dépôt.

## Lancer avec Docker

Prérequis : Docker avec le plugin Docker Compose (avec Colima sur macOS, démarrer le moteur si nécessaire). Aucun JDK ni Maven local n’est nécessaire.

Au premier lancement, copier `.env.example` vers `.env`, puis renseigner `POSTGRES_PASSWORD` avec un mot de passe local non vide. `.env` est ignoré par Git et exclu du contexte de construction Docker ; ne pas le committer.

```sh
cp .env.example .env
# Renseigner POSTGRES_PASSWORD dans .env avant la commande suivante.
docker compose up --build -d
```

La tâche `mise run docker:start` exécute la même commande Compose.

La construction utilise Maven et Java 25, exécute les tests, puis copie uniquement le JAR dans une image Java d’exécution avec un utilisateur non-root. Le premier build nécessite un accès Internet pour télécharger les images et les dépendances Maven.

L’API est accessible sur `http://localhost:8080` (port publié uniquement sur la machine locale) :

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8080/students
```

Consulter les logs et arrêter l’application :

```sh
docker compose logs -f app
docker compose down
```

Compose démarre **PostgreSQL 17** et attend son contrôle de santé avant de lancer l’application avec le profil Spring `postgres`. La connexion utilise `postgres:5432/students`, l’utilisateur `students` et le mot de passe de `.env`. Le port PostgreSQL n’est pas publié sur l’hôte ; seule l’API est exposée sur `127.0.0.1:8080`. La console H2 est désactivée.

- Le volume nommé `postgres-data` conserve les données après un redémarrage, une reconstruction de l’application ou `docker compose down` (`mise run docker:stop`). **Ne pas utiliser `docker compose down -v` sauf pour effacer volontairement la base.**
- Au premier lancement, la base est vide : les scripts H2 `schema.sql` et `data.sql` ne sont pas exécutés. Pour ajouter des étudiants de démonstration après démarrage : `mise run seed`. Les anciennes données H2 ne sont pas migrées.
- `src/main/resources/application-postgres.properties` utilise `spring.jpa.hibernate.ddl-auto=update` pour créer/adapter les tables à partir des entités, avec leurs contraintes d’unicité. Ce choix convient au développement local ; utiliser des migrations versionnées avant une mise en production ou une évolution complexe du schéma.
- Le mot de passe PostgreSQL est initialisé à la création du volume. Modifier `.env` seul ne change pas celui d’une base existante : effectuer aussi la rotation côté PostgreSQL, sans supprimer le volume si les données doivent être conservées.
- Les tests Maven et `mise run start` continuent d’utiliser **H2 en mémoire**, sans nécessiter PostgreSQL. Leurs données sont perdues à l’arrêt. `PostgresProfileTests` vérifie la configuration du profil et le schéma généré sur H2 en mode PostgreSQL ; ce n’est pas un test sur un serveur PostgreSQL réel.

Pour consulter la base depuis son conteneur :

```sh
docker compose exec postgres psql -U students -d students
```

Pour utiliser Docker sans Compose, avec H2 en mémoire :

```sh
docker build -t spring-rest-service .
docker run --rm -p 127.0.0.1:8080:8080 -e SPRING_H2_CONSOLE_ENABLED=false spring-rest-service
```

## Documentation OpenAPI et Scalar

La spécification **OpenAPI 3.1** est générée automatiquement à l’exécution depuis les contrôleurs et les modèles par `springdoc-openapi` **3.0.3** (branche compatible avec Spring Boot 4.0). L’interface interactive est **Scalar 0.5.55**, sans Swagger UI.

Démarrer l’application avec `mise run start` ou `mise run docker:start` (pas les deux simultanément), puis ouvrir :

| Adresse                                                                          | Contenu                                         |
| -------------------------------------------------------------------------------- | ----------------------------------------------- |
| [http://localhost:8080/docs](http://localhost:8080/docs)                         | Documentation interactive Scalar et client HTTP |
| [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)           | Spécification OpenAPI JSON                      |
| [http://localhost:8080/v3/api-docs.yaml](http://localhost:8080/v3/api-docs.yaml) | Spécification OpenAPI YAML                      |

Les dix opérations de `/students` et `/courses` sont documentées avec les modèles, les contraintes des cours, les réponses sans corps et les conflits d’unicité HTTP 409. Les routes techniques (Actuator, H2 et documentation) ne sont pas incluses. Les annotations `io.swagger.v3.oas.annotations` décrivent le contrat OpenAPI ; elles n’installent pas Swagger UI et ne changent pas le comportement des endpoints.

Le JavaScript Scalar est embarqué dans le JAR et servi par l’application, sans CDN. La télémétrie et les polices distantes par défaut sont désactivées. Aucun compte ni clé API n’est nécessaire. Le client HTTP intégré appelle réellement l’API : les POST, PUT et DELETE modifient les données de la base active (PostgreSQL persistante avec Compose, H2 en mémoire en local).

Pour exporter la spécification, avec l’application déjà démarrée :

```sh
curl --fail --silent --show-error http://localhost:8080/v3/api-docs -o openapi.json
curl --fail --silent --show-error http://localhost:8080/v3/api-docs.yaml -o openapi.yaml
```

La génération est dynamique : le build ne produit pas de fichier OpenAPI statique. Les exports sont des instantanés à régénérer après une évolution de l’API.

Configuration dans `src/main/resources/application.properties`. Pour désactiver la documentation dans un environnement, définir **les deux** propriétés `springdoc.api-docs.enabled=false` et `scalar.enabled=false` (ou les variables `SPRINGDOC_API_DOCS_ENABLED=false` et `SCALAR_ENABLED=false`). La documentation est accessible sans authentification, comme l’API ; protéger ou désactiver ces routes avant une exposition publique.

`OpenApiTests` vérifie les routes et modèles générés, les statuts HTTP documentés, le YAML, la page Scalar et son bundle JavaScript local. Test ciblé : `mise exec -- mvn -Dtest=OpenApiTests test`.

## API des cours

La ressource `Course` est disponible sur `/courses`, sans relation avec les étudiants. Son identifiant est généré par le serveur.

| Champ JSON | Type            | Contraintes                                     |
| ---------- | --------------- | ----------------------------------------------- |
| `titre`    | Texte           | Obligatoire, non vide et non blanc              |
| `code`     | Texte           | Obligatoire, non vide et non blanc, unique      |
| `date`     | Date sans heure | Obligatoire, date valide au format `YYYY-MM-DD` |

Les textes sont conservés tels quels (pas de normalisation). H2 conserve ces données uniquement en mémoire ; aucun cours n’est préchargé.

| Route                  | Succès                              | Erreurs principales                                              |
| ---------------------- | ----------------------------------- | ---------------------------------------------------------------- |
| `GET /courses`         | 200, liste JSON                     |                                                                  |
| `GET /courses/{id}`    | 200, objet JSON                     | 404 si absent                                                    |
| `POST /courses`        | 201, en-tête `Location`, corps vide | 400 si invalide ou identifiant fourni ; 409 si code déjà utilisé |
| `PUT /courses/{id}`    | 204, corps vide                     | 400 si invalide ; 404 si absent ; 409 si code déjà utilisé       |
| `DELETE /courses/{id}` | 204, corps vide                     | 404 si absent                                                    |

Les erreurs gérées utilisent `ProblemDetail`. PUT remplace les trois champs : aucun ne peut être omis. L’identifiant du chemin prévaut sur celui du corps. Un cours peut conserver son propre code lors d’une modification.

Application démarrée, créer un cours :

```sh
curl -i -X POST http://localhost:8080/courses \
  -H 'Content-Type: application/json' \
  -d '{"titre":"Introduction à Java","code":"JAVA-101","date":"2026-10-16"}'
curl http://localhost:8080/courses
```

Utiliser l’identifiant renvoyé dans `Location` pour les opérations suivantes (ici `1` à titre d’exemple) :

```sh
curl http://localhost:8080/courses/1
curl -i -X PUT http://localhost:8080/courses/1 \
  -H 'Content-Type: application/json' \
  -d '{"titre":"Java avancé","code":"JAVA-101","date":"2026-11-01"}'
curl -i -X DELETE http://localhost:8080/courses/1
```

Validation ciblée : `mise exec -- mvn -Dtest=CourseTests test`. Validation complète : `mise run verify`.

## Keep Learning Every Day

- **1:** [FOLLOW](https://links.in28minutes.com/lin) Ranga on LinkedIn

## Check Out Our Amazing ROADMAPS

- **1:** [AWS Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#aws-roadmap)
- **2:** [Azure Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#azure-roadmap)
- **3:** [Google Cloud Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#google-cloud-roadmap)
- **4:** [Cloud Beginner Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#cloud-beginner-roadmap)
- **5:** [DevOps Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#devops-roadmap)
- **6:** [Java Full Stack Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#java-full-stack-roadmap)
- **7:** [Java Microservices Roadmap](https://github.com/in28minutes/roadmaps/blob/main/README.md#java-microservices-roadmap)

## Example of Complete Code

### /pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.0</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.in28minutes.springboot.rest.example</groupId>
    <artifactId>spring-boot-2-rest-service-basic</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>spring-boot-2-rest-service</name>
    <description>Spring Boot 2 and REST - Example Project</description>

    <properties>
        <java.version>25</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

---

### /src/main/java/com/in28minutes/springboot/rest/example/SpringBoot2RestServiceApplication.java

```java
package com.in28minutes.springboot.rest.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBoot2RestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot2RestServiceApplication.class, args);
    }
}
```

---

### /src/main/java/com/in28minutes/springboot/rest/example/student/Student.java

```java
package com.in28minutes.springboot.rest.example.student;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Student {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String passportNumber;

}

```

---

### /src/main/java/com/in28minutes/springboot/rest/example/student/StudentNotFoundException.java

```java
package com.in28minutes.springboot.rest.example.student;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(String exception) {
        super(exception);
    }

}
```

---

### /src/main/java/com/in28minutes/springboot/rest/example/student/StudentRepository.java

```java
package com.in28minutes.springboot.rest.example.student;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<@NonNull Student, @NonNull Long> {

}

```

---

### /src/main/java/com/in28minutes/springboot/rest/example/student/StudentResource.java

```java
package com.in28minutes.springboot.rest.example.student;

import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
public class StudentResource {

    private final StudentRepository studentRepository;

    public StudentResource(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping("/students")
    public List<Student> retrieveAllStudents() {
        return studentRepository.findAll();
    }

    @GetMapping("/students/{id}")
    public Student retrieveStudent(@PathVariable long id) {
        Optional<Student> student = studentRepository.findById(id);

        if (student.isEmpty())
            throw new StudentNotFoundException("id-" + id);

        return student.get();
    }

    @DeleteMapping("/students/{id}")
    public void deleteStudent(@PathVariable long id) {
        studentRepository.deleteById(id);
    }

    @PostMapping("/students")
    public ResponseEntity<@NonNull Student> createStudent(@RequestBody Student student) {
        var savedStudent = studentRepository.save(student);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedStudent.getId())
                .toUri();

        return ResponseEntity.created(location)
                .build();

    }

    @PutMapping("/students/{id}")
    public ResponseEntity<@NonNull Student> updateStudent(@RequestBody Student student,
                                                          @PathVariable long id) {

        Optional<Student> studentDetails = studentRepository.findById(id);

        if (studentDetails.isEmpty())
            return ResponseEntity.notFound().build();

        student.setId(id);
        studentRepository.save(student);
        return ResponseEntity.noContent()
                .build();
    }
}

```

---

### /src/main/resources/application.properties

```properties
# Enabling H2 Console
spring.h2.console.enabled=true
#Turn Statistics on
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=debug
# Show all queries
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.type=trace
spring.datasource.url=jdbc:h2:mem:testdb
spring.data.jpa.repositories.bootstrap-mode=default
```

---

### /src/main/resources/data.sql

```
insert into student
values(10001,'Ranga', 'E1234567');

insert into student
values(10002,'Ravi', 'A1234568');
```

---

### /src/main/resources/schema.sql

```
create table student
(
   id integer not null,
   name varchar(255) not null,
   passport_number varchar(255) not null,
   primary key(id)
);
```

---

### /src/test/java/com/in28minutes/springboot/rest/example/SpringBoot2RestServiceApplicationTests.java

```java
package com.in28minutes.springboot.rest.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SpringBoot2RestServiceApplicationTests {

    @Test
    public void contextLoads() {
    }

}
```

---
