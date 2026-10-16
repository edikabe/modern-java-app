---
name: java-code-review
description: Effectuer une revue de code de changements locaux, d’un commit ou d’une branche dans ce projet Java/Spring Boot. Utiliser pour rechercher des bugs, régressions, risques de sécurité et lacunes de tests avant intégration ; produire des constats priorisés et étayés, sans modifier le code sauf demande explicite.
---

# Revue de code Java / Spring Boot

## 1. Définir le périmètre sans modifier le dépôt

- Répondre en français. Lire les instructions AGENTS.md applicables et prendre les versions du pom.xml comme référence, pas le nom du répertoire.
- Respecter le périmètre demandé : fichiers, changements locaux, commit ou branche. Sans précision, examiner les changements locaux suivis et les fichiers non suivis pertinents. S’il n’y a aucun changement, demander quel périmètre revoir plutôt que lancer un audit complet.
- Commencer depuis la racine du projet par `git --no-optional-locks status --short`, puis `git --no-pager diff --stat`, `git --no-pager diff` et `git --no-pager diff --cached`. Les fichiers non suivis ne figurent pas dans ces diffs : les découvrir et les lire séparément, sans parcourir les artefacts générés ni afficher des secrets.
- Pour un commit ou une branche, vérifier les références disponibles avant de construire les commandes. Utiliser `git --no-pager show` pour un commit ; pour une branche, identifier la base d’intégration et comparer depuis l’ancêtre commun. Ne pas supposer que la base est main ou master ; demander si elle reste ambiguë. Ne pas inclure les changements locaux dans une revue de branche sans le signaler.
- Si le dépôt Git contient plusieurs projets, limiter la revue au projet courant sauf demande contraire. Annoncer brièvement le périmètre retenu et toute limite de comparaison.
- Ne pas modifier les sources, reformater, installer de dépendances, créer de commit, changer de branche ou réinitialiser le dépôt pour une simple revue. Une demande de revue n’autorise pas une correction automatique.

## 2. Comprendre chaque changement dans son contexte

- Lire les fichiers modifiés et les portions pertinentes de leurs appelants, tests et configurations. Ne pas conclure à partir du diff seul.
- Suivre le chemin utile de bout en bout : requête HTTP → validation et contrôleur → repository → entité et contraintes SQL → réponse ou erreur HTTP.
- Comparer au comportement antérieur et au contrat documenté. Séparer les régressions introduites des problèmes préexistants ; ne pas attribuer ces derniers au changement revu.
- Vérifier les hypothèses dans le code ou la configuration réelle. Ne pas inventer une exigence métier, une faille exploitable, une incompatibilité de version ou un problème de performance.
- Consulter les skills spécialisés si nécessaire : java-build pour la validation, maven-build-debug pour un échec concret et java-dependency-upgrade si une montée de version fait partie du changement.

## 3. Examiner les risques pertinents

Adapter cette liste au diff ; ne pas transformer une petite revue en audit exhaustif.

### Contrats REST et validation

- Vérifier les routes, statuts, corps, en-têtes Location, types JSON et compatibilité avec les clients existants.
- Vérifier les champs absents, null, vides ou blancs, les dates invalides, les identifiants fournis et les ressources absentes. Vérifier que la validation déclarée est réellement déclenchée.
- Vérifier la sémantique de PUT et l’autorité de l’identifiant du chemin. Un POST ne doit pas écraser involontairement une ligne existante.
- Contrôler la traduction des erreurs en ProblemDetail sans exposition de détails SQL ou de données sensibles. Ne pas convertir indistinctement toute erreur d’intégrité en conflit d’unicité.
- Pour les étudiants, préserver l’unicité des passeports et les réponses 409 en création comme en modification.
- Pour les cours, vérifier le contrat défini dans AGENTS.md : champs titre/code non blancs, date obligatoire, rejet d’un identifiant en POST, remplacement des trois champs en PUT, 404 pour GET/PUT/DELETE absent et 409 pour un code dupliqué. Ne pas imposer ce contrat aux autres ressources sans justification.

### JPA, SQL et concurrence

- Vérifier la cohérence entités/schema.sql/data.sql : colonnes, types, nullabilité, génération d’identifiants, données initiales et mode effectif d’initialisation.
- Préserver les contraintes nommées uk_student_passport et uk_course_code. Un contrôle préalable d’existence ne remplace pas une contrainte SQL face à des requêtes concurrentes.
- Vérifier les frontières transactionnelles et le moment du flush : l’erreur doit pouvoir être traduite dans le périmètre prévu et un échec ne doit pas laisser de modification partielle.
- Vérifier qu’une mise à jour conservant sa propre valeur unique reste autorisée et qu’un conflit ne modifie pas l’état existant.
- Si des relations sont concernées, examiner cascades, suppressions, chargement, requêtes N+1 et cycles de sérialisation. Ne signaler un risque de performance qu’avec un chemin d’exécution et un impact plausibles.
- Tenir compte du caractère volontairement en mémoire de H2 ; ne pas présenter l’absence de persistance externe comme une régression.

### Sécurité et configuration

- Chercher les entrées utilisées dangereusement, les champs internes modifiables par le client, les secrets ajoutés et les données sensibles dans les logs ou erreurs. Ne pas reproduire une valeur secrète dans le rapport.
- Examiner les changements d’exposition réseau, de console H2 et d’Actuator. Pour Compose, préserver l’écoute locale et la désactivation de la console H2 prévues par le projet.
- Si le build change, vérifier la cohérence Java/Maven/Spring Boot entre pom.xml, mise.toml et Dockerfile, le respect du BOM et l’utilité des nouvelles dépendances.
- Évaluer les risques dans le contexte réel de cette application ; ne pas exiger une authentification, une couche service, des DTO ou une nouvelle infrastructure par principe.

### Tests et documentation

- Examiner les assertions, pas seulement la présence de tests : comportement nominal, cas limites, erreurs HTTP et état persistant après échec.
- Pour les changements d’unicité, contrôler la création en doublon, la modification en doublon et la conservation de sa propre valeur. Examiner StudentPassportTests et CourseTests selon le périmètre.
- Vérifier l’isolation des données, l’indépendance de l’ordre d’exécution et les tests HTTP sur port aléatoire. Une transaction de test ne rollback pas automatiquement les écritures du serveur sur un autre thread.
- Signaler une lacune de test avec le scénario concret non couvert ; ne pas en faire à elle seule la preuve d’un bug.
- Contrôler la cohérence de README.md et AGENTS.md si le contrat, les commandes ou les conventions changent. Écarter les préférences stylistiques sans conséquence fonctionnelle ou de maintenance démontrable.

## 4. Valider proportionnellement

- Privilégier l’inspection ciblée puis les tests directement concernés, avec les outils déclarés dans mise.toml. Par exemple, pour les passeports : `mise exec -- mvn -Dtest=StudentPassportTests test` ; pour les cours : `mise exec -- mvn -Dtest=CourseTests test`.
- Pour un changement transversal, envisager `mise run verify` après les tests ciblés. Borner la durée des commandes et respecter une éventuelle demande de revue statique uniquement.
- Ne pas lancer automatiquement un serveur, le seed, Compose ou un arrêt de service. Ne pas exécuter des scripts modifiés non fiables sans en examiner les effets.
- N’ajouter ou ne modifier de test de reproduction que si l’utilisateur autorise les changements ; sinon décrire le scénario ou utiliser les tests existants.
- En cas d’échec, distinguer défaut du changement, problème préexistant et limite d’environnement. Ne pas réparer le dépôt dans le cadre de la revue ni désactiver de tests.
- Rapporter les commandes réellement exécutées et leurs résultats. Si les tests ne peuvent pas être lancés, poursuivre la revue statique et expliciter ce qui reste non vérifié. Des tests réussis ne prouvent pas l’absence de défaut.

## 5. Produire un rapport exploitable

Présenter les constats en premier, par gravité décroissante :

- **P0 — critique** : blocage immédiat, perte de données ou exposition critique démontrée ; réserver aux problèmes indépendants d’hypothèses particulières.
- **P1 — majeur** : régression importante ou risque sérieux dans un scénario réaliste, à corriger avant intégration.
- **P2 — modéré** : défaut réel mais circonscrit, avec conditions et impact identifiés.
- **P3 — mineur** : problème concret de faible impact, sans simples préférences de style.

Pour chaque constat, fournir :

1. Un titre court avec sa priorité, par exemple `[P1] Préserver la contrainte d’unicité à la modification`.
2. Le chemin du fichier et la plus petite plage de lignes utile, vérifiée dans la version revue ; viser une ligne modifiée pour une régression introduite par le diff.
3. Le scénario déclencheur, le comportement observé ou déduit et l’impact utilisateur ou technique.
4. La preuve issue du code ou d’un test ; préciser si le défaut est déduit par inspection et non reproduit.
5. Une piste de correction minimale, sans appliquer de patch non demandé.

Ne pas multiplier les constats pour une même cause racine. Ne pas présenter une hypothèse comme un défaut établi : placer les incertitudes dans une courte section « Questions / limites ». Les éventuels problèmes préexistants importants doivent être explicitement séparés des régressions.

Terminer par le périmètre couvert, les validations effectuées et les risques résiduels ou tests manquants utiles. Si aucun défaut étayé n’est trouvé, écrire « Aucun problème identifié dans le périmètre examiné », puis indiquer les limites ; ne pas garantir que le code est exempt de bugs.
