# Build, tests et outils locaux

Référence projet à lire pour compiler, tester, modifier les tâches mise, utiliser le seed ou diagnostiquer la configuration Java de Zed. Versions et dépendances : `pom.xml` ; outils : `mise.toml`. Commandes depuis la racine.

## Commandes

Privilégier les tâches existantes plutôt que dupliquer les commandes.

| Commande | Action |
| --- | --- |
| `mise install` | Installer les outils déclarés |
| `mise tasks ls` | Lister les tâches disponibles |
| `mise run compile` | Compiler les sources |
| `mise run test` | Tous les tests |
| `mise run test:unit` | Tests unitaires uniquement |
| `mise run test:integration` | Tests d’intégration uniquement |
| `mise run build` | Construire le JAR avec les tests (`mvn package`) |
| `mise run verify` | Cycle Maven jusqu’à `verify` |
| `mise run clean` | Supprimer `target` |
| `mise run rebuild` | Nettoyer puis reconstruire avec les tests |
| `mise run start` | Démarrer Spring Boot en local |
| `mise run deps` | Arbre des dépendances |
| `mise run seed` | Créer 10 étudiants via l’API déjà démarrée |
| `mise run docker:start` | Construire et démarrer Compose en arrière-plan |
| `mise run docker:stop` | Arrêter/supprimer les conteneurs, conserver image et volume |

Après modification de `mise.toml`, vérifier avec `mise tasks ls`. Pour Docker, lire aussi [docker.md](docker.md).

## Tests

- Les tâches `test:unit` et `test:integration` filtrent le tag JUnit `integration` via Surefire (`excludedGroups` / `groups`). `test`, `build`, `rebuild` et `verify` conservent tous les tests par défaut.
- Ne pas confondre absence de tests sélectionnés et validation réussie du comportement : `test:unit` ne teste rien tant qu’aucun test sans tag d’intégration n’existe. Maven compile néanmoins toutes les sources de test.
- Test ciblé : `mise exec -- mvn -Dtest=StudentPassportTests test`. Adapter le nom à une classe existante.
- `PostgresProfileTests` teste le profil `postgres` sur H2 en mode PostgreSQL, sans service externe. Cela ne remplace pas une vérification réelle via Compose : CRUD, conflits HTTP 409 et persistance après redémarrage.

## Seed

`mise/tasks/seed` nécessite `curl` et accepte une URL : `mise run seed -- http://localhost:8080`. Il utilise des passeports fixes : une nouvelle exécution sur une base déjà alimentée peut échouer avec HTTP 409. Il ne supprime pas les données existantes. Ne pas l’exécuter pour valider une modification documentaire.

## Zed / JDT LS

`.zed/settings.json` active la synchronisation Maven via `initialization_options.settings.java.configuration.updateBuildConfiguration = automatic`. Après modification de ces réglages, redémarrer le serveur Java depuis Zed (`editor: restart language server`). Un build Maven en terminal ne resynchronise pas à lui seul le modèle de l’éditeur. Voir aussi la section « Configuration Java dans Zed » du [README](../../README.md).
