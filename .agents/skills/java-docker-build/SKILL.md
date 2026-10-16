---
name: java-docker-build
description: Créer, vérifier ou diagnostiquer une image Docker Java et son lancement Compose, notamment avec Maven, BuildKit, Buildx et Colima sur macOS. Utiliser pour dockeriser une application ou résoudre un build conteneurisé en échec.
---

# Build Docker Java

## Examiner avant de modifier

- Lire AGENTS.md, la configuration de build, Dockerfile, Compose, .dockerignore et les tâches existantes lorsqu’ils sont présents. Pour ce dépôt, lire aussi [la référence Docker/PostgreSQL](../../references/docker.md).
- Identifier le JDK cible, le packaging, le port, les services externes et le stockage. Ne pas ajouter de base externe ou de persistance sans demande.
- Vérifier la CLI Docker, le contexte actif, le moteur, Compose et Buildx selon le problème. Ne pas supposer Docker Desktop.
- Sur macOS avec Colima, ne pas démarrer, arrêter ni changer de contexte sans nécessité. Ne pas arrêter les services des autres projets.

## Construire une configuration minimale

1. Réutiliser les conventions du dépôt. Préférer un build multi-étapes : outil de build et JDK, puis runtime compatible contenant uniquement l’artefact nécessaire.
2. Vérifier l’existence et les architectures des tags retenus auprès des registres ou de leur documentation. Ne pas inventer de tag ; signaler si sa disponibilité n’a pas été vérifiée.
3. Copier d’abord les fichiers de build utiles, puis les sources. Inclure les POM enfants, le wrapper ou les fichiers de configuration si le build en dépend.
4. Pour Maven, utiliser un cache BuildKit sur le dépôt local de l’utilisateur du build. Ne pas intégrer de settings.xml contenant des secrets : employer un montage secret BuildKit si nécessaire.
5. Exécuter la validation prévue par le projet pendant le build. Sélectionner le JAR de façon non ambiguë, notamment si plusieurs artefacts sont produits.
6. Utiliser un utilisateur non-root et une ENTRYPOINT au format exec pour transmettre les signaux à Java. Vérifier les répertoires devant rester inscriptibles.
7. Limiter le contexte avec .dockerignore ; exclure secrets, métadonnées Git et artefacts locaux sans exclure les entrées nécessaires au build.
8. Pour un usage local, publier le port sur 127.0.0.1 par défaut. Ne pas exposer de console d’administration ni élargir le réseau sans demande.
9. Réutiliser `docker compose up --build -d` pour construire et démarrer : un docker build séparé n’est pas nécessaire. Ajouter des tâches seulement si demandé ou prévu dans le périmètre.

## Diagnostiquer Buildx et Colima

- Distinguer plugin Buildx introuvable, moteur inaccessible, builder indisponible et erreur du Dockerfile.
- Sur macOS, Homebrew propose docker-buildx. Vérifier le préfixe Homebrew et les instructions du paquet avant de configurer cliPluginsExtraDirs ; préserver les propriétés existantes de ~/.docker/config.json.
- Examiner `docker buildx version`, `docker buildx ls` et, si nécessaire, `docker buildx inspect --bootstrap`. Le bootstrap peut télécharger une image et démarrer un conteneur.
- Ne créer un builder docker-container que si nécessaire ; expliquer qu’il modifie la configuration Docker. Ne pas forcer amd64 sur une machine ARM64 sans justification.

## Valider et rendre compte

- `docker compose config --quiet` vérifie la configuration, pas le build.
- Un build réussi prouve la création de l’image, pas son démarrage.
- Si le lancement fait partie de la demande, vérifier auparavant les conflits de port ; lancer en arrière-plan puis contrôler les logs et un endpoint existant avec des délais bornés.
- Ne pas inventer de healthcheck utilisant curl ou wget sans vérifier leur présence dans l’image.
- Ne pas lancer automatiquement prune, down -v ou un arrêt global. Signaler les pertes de données liées à une base en mémoire.
- Rapporter séparément validation Compose, build, tests et vérification runtime. Documenter les commandes utiles et les prérequis sans prétendre avoir exécuté ce qui ne l’a pas été.
