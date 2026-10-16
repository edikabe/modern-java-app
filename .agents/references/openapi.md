# Contrat REST, OpenAPI et Scalar

Référence projet à lire lors d’une modification du contrat REST documenté, de la configuration OpenAPI/Scalar ou d’une mise à jour de ces dépendances.

- Springdoc 3.0.x génère OpenAPI sur `/v3/api-docs` et `/v3/api-docs.yaml` ; Scalar fournit l’interface sur `/docs`, sans Swagger UI. `pom.xml` fait référence pour les versions exactes.
- Conserver le JavaScript embarqué ainsi que la désactivation de la télémétrie et des polices distantes.
- Conserver les annotations de contrat et `OpenApiTests` cohérents avec les endpoints, sans modifier leur comportement pour la documentation. Couvrir les contraintes, les réponses sans corps et les conflits d’unicité HTTP 409.
- `OpenApiConfiguration` importe explicitement l’auto-configuration Scalar : Springdoc 3.0 la filtre par défaut. Ne pas remplacer sans vérification par son starter Scalar, qui en 3.0.3 active aussi la confiance aux en-têtes `Forwarded`.
- Configuration dans `src/main/resources/application.properties`. Pour désactiver la documentation, définir **les deux** propriétés `springdoc.api-docs.enabled=false` et `scalar.enabled=false`.
- Aucun export statique n’est généré par le build : la spécification est produite à l’exécution.
- Validation ciblée : `mise exec -- mvn -Dtest=OpenApiTests test`. Ce test ne remplace pas les tests métier des endpoints modifiés.
