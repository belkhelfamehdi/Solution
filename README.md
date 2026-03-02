# TP 1 - Pipeline reactif complet

## Pipeline implemente

La methode `processOrder(OrderRequest request)` de `OrderService` applique les etapes suivantes:

1. Validation de la requete (`Mono.error` si invalide)
2. Transformation `List<String>` -> `Flux<String>`, filtrage des IDs vides/null, limite `take(100)`
3. Chargement des produits via `ProductRepository.findById`, gestion des produits absents et erreurs (`onErrorResume` + ignore)
4. Verification du stock via `getStock` puis filtrage `stock > 0`
5. Application des remises (`map`):
   - `ELECTRONICS` -> 10%
   - Autres categories -> 5%
6. Aggregation via `collectList` puis creation de `Order` final (`COMPLETED`)
7. Gestion globale des erreurs et timeout:
   - `timeout(Duration.ofSeconds(5))`
   - `doOnError` pour logging
   - `onErrorResume` pour retourner une commande `FAILED`
8. Logging detaille avec `doOnNext`, `doOnError`, `doFinally`

## Repository simule

`ProductRepository` contient 5 produits en memoire et simule:
- latence reactive configurable (100ms par defaut)
- erreurs aleatoires configurables (10% par defaut)
- erreurs forcees (utile pour les tests)

## Optimisation bonus implementee

**Option A - Parallelisation**:
- Traitement des IDs produits avec `parallel()` et `Schedulers.parallel()`
- Permet d'accelerer le traitement quand la liste contient plusieurs produits.

## Lancer les tests

```bash
mvn test
```
