# Instructions pour les agents

## Contexte

- Répondre en français et privilégier des modifications ciblées.
- Application REST de gestion d’étudiants et de cours : Spring Boot **4.0.0**, Java **25**, Maven, Spring MVC, Spring Data JPA, H2, Lombok et Actuator.
- Malgré le nom du répertoire, ce projet n’utilise plus Spring Boot 2. Le `pom.xml` fait référence pour les versions et dépendances.
- Les outils locaux sont gérés par `mise` (`mise.toml`). Exécuter les commandes depuis la racine du projet.

## Structure et règles métier

- Code Java : `src/main/java/com/in28minutes/springboot/rest/example/`.
- Package `student` : entité `Student`, repository JPA, contrôleur `StudentResource` et exception métier.
- API : `GET /students`, `GET /students/{id}`, `POST /students`, `PUT /students/{id}`, `DELETE /students/{id}`.
- Un numéro de passeport doit être unique. Conserver la contrainte `uk_student_passport` et la réponse HTTP **409** avec `ProblemDetail` en cas de doublon, à la création comme à la modification.
- Package `course` : CRUD `/courses`, entité `Course`, repository, contrôleur et erreurs `ProblemDetail`. Champs `titre` et `code` obligatoires non blancs, `date` obligatoire au format `YYYY-MM-DD`, sans relation avec les étudiants.
- Conserver l’unicité du code (`uk_course_code`) et HTTP 409 en création/modification. POST refuse un identifiant fourni ; PUT remplace les trois champs et utilise l’identifiant du chemin. GET/PUT/DELETE d’un cours absent renvoient 404. Tests dédiés : `CourseTests`.
- Configuration : `src/main/resources/application.properties`. Garder l’entité JPA, `schema.sql` et `data.sql` cohérents lors des changements de modèle.
- H2 fonctionne en mémoire : les données modifiées sont perdues à chaque redémarrage. Ne pas ajouter de persistance ou de base externe sans demande explicite.
- Tests : `src/test/java/com/in28minutes/springboot/rest/example/`, notamment `StudentPassportTests` pour les conflits de passeport.

## Commandes usuelles

Privilégier les tâches existantes plutôt que dupliquer les commandes :

| Commande                    | Action                                                     |
| --------------------------- | ---------------------------------------------------------- |
| `mise install`              | Installer les outils déclarés                              |
| `mise tasks ls`             | Lister les tâches disponibles                              |
| `mise run compile`          | Compiler les sources                                       |
| `mise run test`             | Exécuter tous les tests (unitaires et intégration)         |
| `mise run test:unit`        | Exécuter uniquement les tests unitaires                    |
| `mise run test:integration` | Exécuter uniquement les tests d’intégration                |
| `mise run build`            | Construire le JAR avec les tests (`mvn package`)           |
| `mise run verify`           | Exécuter le cycle Maven jusqu’à `verify`                   |
| `mise run clean`            | Supprimer `target`                                         |
| `mise run rebuild`          | Nettoyer puis reconstruire avec les tests                  |
| `mise run start`            | Démarrer Spring Boot en local                              |
| `mise run deps`             | Afficher l’arbre des dépendances                           |
| `mise run seed`             | Créer 10 étudiants via l’API déjà démarrée                 |
| `mise run docker:start`     | Construire l’image et démarrer via Compose en arrière-plan |
| `mise run docker:stop`      | Arrêter et supprimer les conteneurs Compose du projet      |

La tâche `seed` se trouve dans `mise/tasks/seed`, nécessite `curl` et accepte une URL en argument : `mise run seed -- http://localhost:8080`. Elle utilise des passeports fixes : une nouvelle exécution sur une base déjà alimentée peut échouer avec HTTP 409. Elle ne supprime pas les données existantes.

## GitHub Actions

- `.github/workflows/ci.yml` exécute `mvn -B -ntp verify` à chaque push sur toutes les branches, sans exécution sur les tags ni exécution supplémentaire sur les événements de pull request.
- Le job utilise `ubuntu-24.04`, Java 25 (Temurin via `actions/setup-java`) et Maven préinstallé ; il exécute tous les tests et construit le JAR. Ne pas désactiver les tests ni ajouter de build Docker ou de déploiement sans demande.
- Conserver le cache Maven, les rapports Surefire / JUnit téléchargeables même en cas d’échec et le JAR après succès (artefacts conservés sept jours).
- Limiter les permissions à `contents: read`. Aucun secret applicatif ni service de base externe n’est requis.
- Après modification, vérifier le YAML et utiliser `actionlint` s’il est disponible ; une validation locale ne prouve pas l’exécution sur le runner GitHub.

## Docker et Colima

- Environnement macOS avec **Colima**, pas de dépendance obligatoire à Docker Desktop.
- Prérequis : moteur Colima démarré, CLI Docker, plugin Compose et plugin Buildx/BuildKit fonctionnels.
- Si nécessaire : `colima start` puis `docker context use colima`. Ne pas changer de contexte ni arrêter Colima sans nécessité.
- `docker compose up --build -d` construit déjà l’image : pas besoin d’un `docker build` séparé avant `mise run docker:start`.
- Le `Dockerfile` utilise Maven/Java 25 pour exécuter `verify`, un cache BuildKit pour Maven, puis une image JRE 25 avec un utilisateur non-root.
- `.dockerignore` limite le contexte à `pom.xml` et `src` ; l’adapter si le build nécessite de nouveaux fichiers.
- Compose publie uniquement `127.0.0.1:8080` et désactive la console H2. Ne pas élargir l’exposition réseau sans demande.
- Logs : `docker compose logs -f app`.
- Vérification manuelle : `curl http://localhost:8080/actuator/health` et `curl http://localhost:8080/students`.
- `docker:stop` exécute `docker compose down` : l’image est conservée et Colima reste démarré.
- Ne pas lancer simultanément l’application locale et Compose sur le même port 8080.

## Conventions de modification et validation

- Respecter le style du fichier modifié, les imports Jakarta, Lombok et l’injection par constructeur déjà utilisée.
- Éviter les dépendances supplémentaires, les refontes et les changements sans rapport avec la demande.
- Préserver les modifications utilisateur ; ne pas créer de commit ni de branche sans demande.
- Ne pas modifier les artefacts générés dans `target/` ni ajouter de secrets au dépôt ou aux images.
- Ajouter ou adapter les tests lors d’un changement de comportement, notamment pour les contraintes de données et les statuts HTTP.
- Annoter les classes de tests d’intégration avec `@Tag("integration")` (contexte Spring, base de données ou serveur HTTP). Les tests unitaires ne portent pas ce tag. Les trois classes actuelles sont des tests d’intégration ; la tâche `test:unit` n’exécute donc aucun test tant qu’aucun test unitaire n’est ajouté.
- Les tâches `test:unit` et `test:integration` filtrent les tags JUnit via Maven Surefire (`excludedGroups` / `groups`). `test`, `build`, `rebuild` et `verify` conservent tous les tests par défaut.
- Pour un test ciblé : `mise exec -- mvn -Dtest=StudentPassportTests test`. Pour une validation complète : `mise run verify`.
- Après modification de `mise.toml`, vérifier avec `mise tasks ls`. Après modification de Compose, utiliser `docker compose config --quiet` ; cela ne valide pas la construction ni le démarrage de l’image.
- Ne pas lancer automatiquement les tâches longues de serveur, le seed ou les arrêts de services pour valider une simple modification documentaire.
- Mettre à jour `README.md` et ce fichier lorsque les commandes ou conventions changent.
- Terminer par un bref résumé des fichiers modifiés et des vérifications réellement exécutées. Signaler explicitement les validations impossibles ou non exécutées.
