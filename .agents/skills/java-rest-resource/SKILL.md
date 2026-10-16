---
name: java-rest-resource
description: Ajouter une nouvelle ressource REST complète dans ce projet Spring Boot, de l’entité JPA aux endpoints CRUD, avec repository, contraintes SQL, erreurs HTTP, tests et documentation. Utiliser pour créer une ressource métier ou exposer une nouvelle entité via une API ; ne pas générer de ressource lors d’une simple demande d’explication.
---

# Ajouter une ressource REST de bout en bout

## 1. Examiner le projet et cadrer le besoin

- Lire les instructions AGENTS.md applicables, le pom.xml et les tâches mise avant toute modification. Les versions et conventions du dépôt font référence.
- Examiner les fichiers actuels du package `src/main/java/com/in28minutes/springboot/rest/example/student/`, notamment Student, StudentRepository, StudentResource et StudentNotFoundException. Lire les tests existants, dont StudentPassportTests, ainsi que application.properties, schema.sql et data.sql dans src/main/resources.
- Examiner les éventuels gestionnaires d’erreurs et autres ressources déjà ajoutées. Réutiliser les conventions actuelles plutôt que recopier un modèle figé.
- Identifier les modifications utilisateur et les préserver. Ne pas refondre Student pour ajouter une autre ressource.
- Déduire de la demande le nom métier, le package et le chemin HTTP au pluriel, les champs et leurs types, les champs obligatoires, les valeurs par défaut, les contraintes d’unicité, les relations et les opérations souhaitées.
- Demander en une fois les informations métier indispensables qui manquent. Ne pas inventer des champs, des relations, une politique de suppression ou des contraintes d’unicité. Proposer des choix explicites si le besoin est encore vague.
- Résumer le contrat retenu et les fichiers à créer/modifier. Pour une demande de CRUD complet sans restriction, couvrir les cinq opérations ci-dessous ; pour un sous-ensemble explicite, respecter ce périmètre.

## 2. Définir le contrat HTTP

Utiliser le chemin de collection retenu, sans laisser ces exemples génériques dans le code final :

| Opération | Réponse attendue |
| --- | --- |
| GET collection | 200 avec une liste, éventuellement vide |
| GET élément par identifiant | 200 ou 404 |
| POST collection | 201 avec un en-tête Location vers la ressource créée |
| PUT élément par identifiant | 204 après modification, 404 si absent |
| DELETE élément par identifiant | 204 après suppression ; définir explicitement le comportement si absent |

- Suivre le format de réponse existant pour POST : ne pas ajouter un corps ou changer le contrat sans raison.
- Définir la sémantique PUT pour les champs omis et null. Ne pas implémenter implicitement une mise à jour partielle ou ajouter PATCH sans demande.
- L’identifiant du chemin fait autorité pour PUT. Pour POST, empêcher qu’un identifiant fourni permette de modifier une ligne existante : le rejeter ou l’ignorer selon le contrat retenu, et tester ce comportement.
- Utiliser 400 pour les entrées invalides selon les règles retenues, 404 pour les éléments absents et 409 pour les conflits métier identifiés. Employer ProblemDetail pour les erreurs gérées ; ne pas exposer de détails SQL internes.

## 3. Implémenter la chaîne applicative

### Modèle et persistance

- Créer un package métier sous `src/main/java/com/in28minutes/springboot/rest/example/`.
- Ajouter l’entité JPA avec imports Jakarta, identifiant généré et conventions Lombok du dépôt. Utiliser des noms de colonnes explicites lorsque nécessaire.
- Ajouter un repository Spring Data JPA avec le type d’identifiant correspondant. N’ajouter des méthodes de recherche que si les opérations les nécessitent.
- Traduire les contraintes métier en contraintes de base nommées, notamment l’unicité. Un contrôle préalable existsBy ne remplace pas une contrainte SQL, car il ne protège pas des accès concurrents.
- Garder l’entité et schema.sql cohérents : types, nullabilité, colonnes, identifiants, séquences éventuelles et contraintes. Vérifier le mode réel d’initialisation JPA/SQL avant de modifier les scripts.
- Adapter data.sql seulement si des exemples initiaux sont utiles ou requis. Employer des colonnes explicites et des identifiants/valeurs compatibles avec les contraintes et la génération des identifiants. Préserver les données des autres ressources.
- Pour les relations, clarifier cardinalité, propriété, suppressions et cascades ; éviter les cycles de sérialisation et les cascades destructives implicites.

### API et logique métier

- Ajouter un contrôleur `<Nom>Resource` et l’injection par constructeur selon le style existant.
- Ne pas imposer une couche service pour un CRUD trivial : ce projet utilise directement le repository dans StudentResource. Introduire un service seulement si une logique métier ou une transaction couvrant plusieurs opérations le justifie.
- N’introduire DTO et mapper que si le contrat d’entrée/sortie, les champs protégés ou les relations le justifient ; ne pas ajouter de dépendance de mapping par défaut.
- Vérifier la présence du support Bean Validation avant d’utiliser @Valid et les contraintes Jakarta Validation. Ajouter une dépendance seulement si elle est nécessaire et cohérente avec le parent/BOM du projet.
- Gérer l’absence d’élément explicitement. Pour les conflits de données, identifier la contrainte connue avant de produire 409 ; ne pas convertir toutes les DataIntegrityViolationException en conflit d’unicité.
- S’assurer que les exceptions de contrainte surviennent dans un périmètre où elles peuvent être traduites en réponse HTTP, notamment en cas de transaction et de flush différé.
- Respecter le style local, les annotations de nullabilité et les conventions de nommage. Ne pas ajouter pagination, authentification, base externe ou infrastructure sans demande.

## 4. Ajouter les tests

Suivre les outils de test déjà présents. Le projet dispose de tests SpringBootTest avec port aléatoire et client HTTP Java ; ne pas ajouter un framework uniquement pour reproduire ces capacités.

Couvrir selon le contrat :

- création valide : statut 201, Location correcte et ressource relisible ;
- liste et lecture individuelle ;
- modification valide : statut et valeurs persistées ;
- suppression : statut et disparition vérifiée ;
- lecture/modification d’un identifiant absent et politique retenue pour DELETE absent ;
- entrées invalides et gestion des identifiants fournis ;
- si unicité : doublon à la création et à la modification → 409, état conservé après échec, modification conservant sa propre valeur autorisée ;
- relations ou contraintes métier spécifiques, si présentes.

Isoler les tests des données initiales et de l’ordre d’exécution : données uniques, base H2 de test distincte ou nettoyage ciblé selon les conventions. Attention : une transaction sur la méthode de test ne rollback pas automatiquement les écritures effectuées par un serveur HTTP sur un autre thread. Ne pas supprimer les données des autres suites.

## 5. Documenter et valider

- Mettre à jour README.md avec les routes, les champs attendus, les statuts utiles et des exemples curl exécutables avec le vrai nom de ressource.
- Mettre à jour AGENTS.md si la structure ou les règles du projet changent. Ne pas dupliquer toute cette procédure dans ce fichier.
- Ne modifier le seed que si demandé ; s’il change, documenter ses effets et son comportement en cas de nouvelle exécution.
- Exécuter les tests ciblés avec les noms réels des nouvelles classes via les outils du dépôt, puis `mise run verify` si possible. Le skill java-build peut guider cette validation.
- Ne pas lancer automatiquement le serveur, Compose ou le seed pour générer une ressource. Les tests HTTP sur port aléatoire suffisent généralement.
- Si un contrôle échoue, distinguer régression introduite et problème préexistant ; ne pas désactiver de tests pour obtenir un succès.

## Critères de fin

La ressource demandée est reliée de bout en bout : HTTP → logique éventuelle → repository → modèle/contraintes SQL, avec traduction des erreurs, tests du contrat et documentation cohérente. Aucun placeholder ne subsiste. Les contraintes existantes, notamment celles des passeports étudiants, sont préservées.

Terminer avec les fichiers créés/modifiés, les routes ajoutées, les décisions de contrat importantes, les commandes de validation et leurs résultats réels. Indiquer explicitement ce qui n’a pas pu être vérifié.
