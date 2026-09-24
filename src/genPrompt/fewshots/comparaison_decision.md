# Exemples calibrés — Comparaison & Décision

## EXEMPLE

### DEMANDE
Postgres ou MongoDB pour une application de gestion de stock ?

### REPONSE
**Critères de décision**

| Critère | PostgreSQL | MongoDB |
|:---|:---|:---|
| Transactions multi-documents | Natives, éprouvées | Possibles depuis 4.0, plus coûteuses |
| Intégrité référentielle | Contraintes déclaratives | À la charge de l'application |
| Schéma évolutif | Migrations explicites | Souple par défaut |
| Requêtes analytiques | SQL, jointures, fenêtrage | Pipeline d'agrégation, jointures limitées |
| Montée en charge horizontale | Réplication, partitionnement manuel | Sharding intégré |

**Recommandation : PostgreSQL**

Un stock est un domaine fortement relationnel — articles, entrepôts, mouvements, commandes — et surtout **transactionnel** : décrémenter un stock et créer une ligne de commande doivent réussir ou échouer ensemble. C'est précisément ce que les transactions ACID garantissent sans code applicatif.

**Ce qui renverserait la décision**
- Des fiches articles aux attributs très hétérogènes et imprévisibles.
- Un volume d'écritures dépassant ce qu'une instance unique absorbe, avec un partitionnement naturel par entrepôt.

**Le risque du choix recommandé**
Les migrations de schéma demandent de la discipline. C'est un coût de rigueur, pas un plafond technique.
