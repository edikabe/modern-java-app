---
name: maven-build-debug
description: Diagnostiquer un build Maven en échec, une dépendance introuvable, une incompatibilité JDK, un problème Lombok ou une différence entre local, CI et Docker. Utiliser à partir d’une erreur concrète, sans refonte ni suppression aveugle des caches.
---

# Diagnostiquer un build Maven

## Collecter les preuves

1. Lire les règles du dépôt, les POM concernés et les tâches de build. Préserver les modifications existantes.
2. Relever la commande, le répertoire, les profils actifs et le premier message causal utile, pas seulement le résumé BUILD FAILURE.
3. Reproduire avec la commande du projet et un timeout adapté. Ne pas relancer un échec coûteux si le journal contient déjà la cause suffisante.
4. Comparer le JDK et Maven réellement utilisés, le wrapper, les toolchains, les paramètres de compilation, la CI et les images Docker. Ne pas conclure à partir de java -version seul.

## Classer et investiguer

- **Résolution réseau** : identifier l’artefact, sa version et le dépôt ; distinguer DNS, proxy, authentification, certificat, rate limit et version inexistante. Ne pas désactiver TLS ni afficher les identifiants de settings.xml.
- **Dépendances** : utiliser `dependency:tree`, puis un filtre `-Dincludes=groupId:artifactId` fondé sur l’erreur. Examiner la gestion du parent et des BOM avant d’ajouter une version explicite.
- **Configuration** : utiliser `help:effective-pom` ou `help:active-profiles` lorsque l’héritage est ambigu. Limiter la sortie ; ne pas exposer les secrets des propriétés ou configurations Maven.
- **Compilation** : contrôler release/source/target, les API disponibles dans le JDK cible et les packages javax/jakarta. Pour Lombok, vérifier la compatibilité JDK, les annotationProcessorPaths et l’activation de l’annotation processing avant de toucher au code généré.
- **Tests** : consulter les rapports présents dans target/surefire-reports et target/failsafe-reports du module concerné. Isoler le test fautif ; distinguer assertion, initialisation du contexte, port occupé et service externe manquant.
- **Packaging et démarrage** : vérifier le plugin de packaging, le repackage Spring Boot et le manifeste si nécessaire. Un ClassNotFoundException au runtime n’est pas une erreur de compilation.
- **Local contre CI/Docker** : comparer versions, architecture, profils, variables non sensibles, fichiers exclus du contexte et services disponibles.

Préfixer ces objectifs Maven par la commande réellement utilisée dans le projet (tâche, wrapper ou mise). Commencer sans `-X` ; utiliser `-e` si nécessaire. Les traces de debug peuvent contenir des secrets : ne pas les publier brutes.

## Corriger et confirmer

- Formuler une hypothèse reliée aux preuves, puis appliquer la correction minimale.
- Ne pas supprimer tout ~/.m2, modifier une version au hasard, désactiver un test ou utiliser un retry comme correction durable.
- Si un artefact de cache est démontré corrompu, proposer une réparation ciblée et expliquer son impact avant toute suppression.
- Relancer d’abord le cas ciblé, puis la validation pertinente. Ne pas corriger les échecs sans rapport.
- Conclure avec cause confirmée ou hypothèse restante, fichiers modifiés, commandes exécutées et éventuels blocages.
