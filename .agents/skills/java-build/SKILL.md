---
name: java-build
description: Compiler, tester et construire un projet Java Maven avec les outils du dépôt. Utiliser pour une demande de compilation, validation, construction de JAR ou vérification avant livraison ; ne couvre pas le déploiement.
---

# Construire un projet Java Maven

## Identifier le contexte

- Lire les règles AGENTS.md applicables et suivre la langue du projet. Pour ce dépôt, lire aussi [la référence build/tests/Zed](../../references/build.md).
- Repérer le pom.xml racine, les modules concernés, le Maven Wrapper et les tâches existantes (mise.toml, Makefile, CI). Ne pas supposer leurs chemins ou leur présence.
- Déterminer les versions Java et Maven depuis le dépôt, les propriétés du compilateur, les toolchains et la CI. Ne pas imposer une version universelle.
- Vérifier les modifications existantes avant toute édition ; les préserver.
- Si le projet utilise Gradle, ne pas lui ajouter Maven : utiliser ses tâches natives et signaler que les exemples Maven ci-dessous ne s’appliquent pas.

## Choisir et exécuter la validation

1. Privilégier une tâche du dépôt correspondant à la demande. Sinon, utiliser le wrapper présent, puis Maven disponible dans l’environnement du projet.
2. Vérifier le JDK réellement utilisé avec la commande de version Maven exécutée dans ce même environnement. Tenir compte des toolchains pouvant sélectionner un autre compilateur.
3. Choisir la portée minimale utile : compile pour les sources, un test ciblé pour un changement isolé, package pour le JAR, verify pour inclure les contrôles et tests d’intégration configurés.
4. Exemples à adapter : `mise run compile`, `mise run verify`, `./mvnw -Dtest=StudentPassportTests test`. Le nom de test doit exister dans le projet.
5. Dans un projet multi-module, identifier les dépendances entre modules avant d’utiliser `-pl` et `-am`. Un filtre de test peut échouer dans les modules qui ne contiennent pas ce test : ne pas désactiver globalement les contrôles pour le contourner.
6. Borner la durée des commandes. Ne pas lancer un serveur, une publication ou un déploiement pour valider une simple compilation.
7. En cas d’échec, distinguer environnement, téléchargement, compilation, tests et packaging. Utiliser maven-build-debug si disponible pour une investigation approfondie.

## Garde-fous et résultat

- Ne pas sauter les tests, lancer clean systématiquement, supprimer le cache Maven ou modifier les versions pour obtenir artificiellement un succès.
- Ne pas éditer les fichiers générés dans target ni installer globalement des outils sans nécessité.
- Demander les permissions réseau ou d’écriture nécessaires ; ne pas présenter un blocage d’environnement comme une erreur du code.
- Indiquer la commande réellement exécutée, son résultat, les tests effectivement exécutés et les limites. Un timeout n’est pas un succès.
- Donner le chemin de l’artefact uniquement après avoir confirmé son existence. Un package réussi ne prouve pas que l’application démarre.
