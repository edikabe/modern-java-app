# GitHub Actions

Référence projet à lire avant de modifier `.github/workflows/ci.yml` ou les conventions de CI.

- Le workflow exécute `mvn -B -ntp verify` à chaque push sur toutes les branches, sans exécution sur les tags ni exécution supplémentaire sur les événements de pull request.
- Le job utilise `ubuntu-24.04`, Java 25 (Temurin via `actions/setup-java`) et Maven préinstallé. Il exécute tous les tests et construit le JAR. Vérifier les versions dans les fichiers lors d’une migration.
- Ne pas désactiver les tests ni ajouter de build Docker, de publication ou de déploiement sans demande.
- Conserver le cache Maven, les rapports Surefire / JUnit téléchargeables même en cas d’échec et le JAR après succès. Artefacts conservés sept jours.
- Limiter les permissions à `contents: read`. Aucun secret applicatif ni service de base externe n’est requis : les tests utilisent H2.
- Après modification, vérifier le YAML et utiliser `actionlint` s’il est disponible. Une validation locale ne prouve pas l’exécution sur le runner GitHub.
