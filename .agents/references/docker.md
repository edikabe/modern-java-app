# Docker, Colima et PostgreSQL

Référence projet à lire avant de modifier ou lancer Docker/Compose, ou de changer le profil `postgres` et la persistance. Le skill `java-docker-build` décrit la procédure de build et de diagnostic.

## Environnement et construction

- macOS avec Colima, sans dépendance obligatoire à Docker Desktop. Prérequis : moteur démarré, CLI Docker, plugins Compose et Buildx/BuildKit fonctionnels.
- Si nécessaire : `colima start` puis `docker context use colima`. Ne pas changer de contexte ni arrêter Colima sans nécessité.
- `mise run docker:start` utilise `docker compose up --build -d`, qui construit déjà l’image : aucun `docker build` séparé nécessaire.
- Le `Dockerfile` exécute Maven `verify` avec Java 25 et un cache BuildKit Maven, puis utilise une image JRE 25 et un utilisateur non-root. Vérifier les versions dans les fichiers lors d’une migration.
- `.dockerignore` limite le contexte à `pom.xml` et `src` ; l’adapter si le build nécessite d’autres fichiers, sans inclure de secrets.

## Configuration et données

- Compose publie uniquement `127.0.0.1:8080`, désactive la console H2 et active le profil `postgres`. PostgreSQL 17 n’a aucun port publié ; l’application attend son healthcheck. Ne pas élargir l’exposition réseau sans demande.
- Avant Compose, copier `.env.example` vers `.env` s’il n’existe pas et renseigner `POSTGRES_PASSWORD` (obligatoire). `.env` est ignoré par Git et exclu de l’image. Changer cette variable ne change pas le mot de passe d’une base déjà initialisée.
- `src/main/resources/application-postgres.properties` désactive la console et les scripts SQL H2 ; `ddl-auto=update` crée/adapte le schéma pour le développement local. La base démarre vide, sans données de démonstration. Prévoir des migrations versionnées pour la production ; conserver les contraintes uniques dans les entités.
- Le volume `postgres-data` conserve la base. Ne jamais exécuter `down -v` ni supprimer le volume sans demande explicite.
- `mise run docker:stop` exécute `docker compose down` : image et volume conservés, Colima reste démarré.
- H2 reste la base par défaut en local/tests (`src/main/resources/application.properties`), en mémoire : données perdues au redémarrage. Ne pas basculer ces usages vers PostgreSQL sans demande.
- Ne pas lancer simultanément l’application locale et Compose sur le port 8080.

## Validation

- Après modification de Compose : `docker compose config --quiet`. Cela ne valide ni la construction ni le démarrage.
- Si le lancement est demandé, vérifier `http://localhost:8080/actuator/health` et `/students` avec `curl`, puis les comportements concernés (CRUD, conflits 409, persistance après redémarrage).
- `PostgresProfileTests` utilise H2 en mode PostgreSQL, pas un vrai serveur PostgreSQL ; ne pas présenter ce test comme une validation réelle de Compose.
- Logs ponctuels : `docker compose logs app` ; suivi manuel : `docker compose logs -f app`. Ne pas lancer de suivi indéfini pour une validation automatisée.
