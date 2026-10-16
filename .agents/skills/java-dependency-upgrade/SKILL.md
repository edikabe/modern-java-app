---
name: java-dependency-upgrade
description: Préparer et effectuer une montée de version Java, Maven, Spring Boot ou de dépendances Maven. Utiliser pour une migration ou une mise à jour demandée, avec vérification de compatibilité, respect des BOM et validation progressive.
---

# Mettre à jour les dépendances Java

## Définir la cible et les contraintes

1. Lire les règles du dépôt, les POM, les toolchains, le wrapper, les tâches, la CI et les images Docker concernés.
2. Relever les versions actuelles et la cible demandée. Si la cible est ambiguë et change significativement le périmètre, demander une précision avant de modifier.
3. Vérifier les matrices de compatibilité et notes de migration officielles : JDK, Maven, Spring Boot, plugins, Lombok et bibliothèques concernées. Pour une demande de dernière version, consulter une source actuelle et fiable ; ne pas se fier à la mémoire.
4. Identifier les versions héritées du parent ou gérées par un BOM. Préférer une mise à jour cohérente du parent/BOM à des overrides isolés.
5. Préserver les modifications utilisateur et établir une validation de référence lorsque possible. Distinguer les échecs préexistants des régressions.

## Effectuer une migration ciblée

- Expliquer brièvement les risques, ruptures d’API et changements de comportement pertinents avant une migration majeure.
- Modifier seulement les composants nécessaires à la cible. Ne pas lancer une mise à jour automatique de toutes les dépendances ou des plugins.
- Aligner les versions Java du compilateur, des outils locaux, de la CI et des images de build/runtime lorsque ces fichiers existent.
- Adapter les API et propriétés supprimées en suivant les guides officiels. Pour javax vers jakarta, ne pas faire de remplacement global : certains packages javax appartiennent toujours au JDK.
- Vérifier les changements de modules Spring Boot, les dépendances de test, l’annotation processing et les plugins de packaging.
- Ne pas supprimer une fonctionnalité, un test ou une contrainte métier pour réussir la migration. Ne pas déclarer une vulnérabilité corrigée sans vérifier l’avis et la version effectivement résolue.

## Valider progressivement

1. Compiler puis exécuter les tests ciblant les comportements affectés.
2. Exécuter les tests et contrôles complets du dépôt, généralement le cycle verify.
3. Examiner l’arbre de dépendances pour les conflits ou overrides restants si la migration le justifie.
4. Si le packaging ou le runtime change, vérifier le JAR ou l’image et, si autorisé dans le périmètre, le démarrage avec des contrôles bornés.
5. Mettre à jour la documentation et les instructions du dépôt lorsque versions, commandes ou prérequis changent.

Conclure avec les versions avant/après, les fichiers modifiés, les adaptations importantes, les sources de compatibilité consultées et les validations réellement exécutées. Signaler clairement toute vérification bloquée et tout risque restant. Ne pas publier d’artefact ni créer de commit sans demande.
