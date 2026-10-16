# Instructions pour les agents

## Essentiel

- Répondre en français ; modifications ciblées, sans refonte ni dépendance superflue. Préserver le travail utilisateur ; aucun commit ni branche sans demande.
- API étudiants/cours, Spring Boot (pas Boot 2 malgré le nom du dossier), Maven, JPA, Lombok. `pom.xml` fait référence pour les versions ; outils via `mise.toml`. Exécuter les commandes depuis la racine.
- Java : `src/main/java/com/in28minutes/springboot/rest/example/` ; tests : même package sous `src/test/java/`. Respecter le style existant, Jakarta et l’injection par constructeur.
- Ne pas éditer `target/` ni ajouter de secrets. Ne pas élargir l’exposition réseau ni supprimer de volume PostgreSQL (`down -v`) sans demande explicite.
- H2 en mémoire reste la base locale/de test ; Compose utilise PostgreSQL persistant. Ne pas basculer le local/les tests vers PostgreSQL sans demande.

## Invariants métier

- CRUD `/students` et `/courses` dans les packages `student` et `course`. Conserver les contraintes uniques `uk_student_passport` et `uk_course_code`, avec HTTP 409 et `ProblemDetail` en création **et** modification.
- Cours : `titre` et `code` non blancs, `date` obligatoire au format `YYYY-MM-DD`, aucune relation étudiant. POST refuse un ID fourni ; PUT remplace les trois champs et utilise l’ID du chemin. GET/PUT/DELETE d’un cours absent : 404.
- Garder entités JPA, `src/main/resources/schema.sql` et `data.sql` cohérents. Préserver les contraintes d’unicité dans les entités, aussi utilisées par PostgreSQL.
- Adapter les tests aux changements de comportement : notamment `StudentPassportTests`, `CourseTests` et `OpenApiTests`. La documentation doit refléter les endpoints, pas modifier leur comportement.

## Validation

- Privilégier les tâches existantes (`mise tasks ls`). Test ciblé : `mise exec -- mvn -Dtest=StudentPassportTests test` ; validation complète : `mise run verify`.
- Tests d’intégration (Spring, base ou serveur HTTP) : `@Tag("integration")` sur la classe ; aucun tag pour les tests unitaires. Ne pas désactiver les tests du build.
- Une modification documentaire ne justifie pas de démarrer/arrêter des services ni d’exécuter le seed.
- Mettre à jour `README.md` et les instructions concernées si les commandes/conventions changent. Terminer par les fichiers modifiés, les vérifications exécutées et leurs limites.

## Références à la demande

Lire uniquement les références pertinentes **avant d’intervenir sur le sujet**, pas toutes au démarrage. Les chemins ci-dessous sont relatifs à la racine du projet. Utiliser aussi les skills disponibles lorsque leur description correspond à la tâche.

| Sujet de la tâche                                                         | Référence à lire                                               |
| ------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Build, tests, tâches mise, seed, synchronisation Java dans Zed            | [.agents/references/build.md](.agents/references/build.md)     |
| Docker, Compose, Colima, profil PostgreSQL ou persistance                 | [.agents/references/docker.md](.agents/references/docker.md)   |
| GitHub Actions / CI                                                       | [.agents/references/ci.md](.agents/references/ci.md)           |
| Contrat REST documenté, Springdoc, Scalar, mise à jour de ces dépendances | [.agents/references/openapi.md](.agents/references/openapi.md) |
