# Exemples calibrés — Conception & Architecture

## EXEMPLE

### DEMANDE
Ecris une fonction Python qui valide un numero IBAN.

### REPONSE
**Analyse (2-3 phrases)**
La validation IBAN est normalisée : longueur variable par pays, réarrangement des 4 premiers caractères en fin de chaîne, conversion des lettres en nombres, puis modulo 97 qui doit valoir 1. Aucune dépendance externe n'est nécessaire.

```python
def iban_valide(iban: str) -> bool:
    """Valide un IBAN par le contrôle modulo 97 (norme ISO 13616)."""
    compact = iban.replace(" ", "").upper()
    if not 15 <= len(compact) <= 34 or not compact.isalnum():
        return False

    # Les 4 premiers caracteres passent en fin de chaine
    reordonne = compact[4:] + compact[:4]
    # Chaque lettre devient sa position dans l'alphabet + 9 (A=10 ... Z=35)
    numerique = "".join(
        str(int(c, 36)) if c.isalpha() else c for c in reordonne
    )
    return int(numerique) % 97 == 1
```

**Points d'attention**
- La longueur exacte dépend du pays ; ce contrôle vérifie la clé, pas l'existence du compte.
- Les espaces de présentation sont tolérés, la casse est normalisée.

## EXEMPLE

### DEMANDE
Propose une arborescence de dossiers pour une API REST Node avec Express et Postgres.

### REPONSE
**Analyse (2-3 phrases)**
Le découpage suit les couches de responsabilité plutôt que les types de fichiers : chaque domaine métier reste localisé, ce qui limite les modifications transverses quand une entité évolue.

```
src/
  routes/        Déclaration des endpoints, aucune logique métier
  controllers/   Validation d'entrée, appel des services, mise en forme HTTP
  services/      Logique métier, indépendante d'Express
  repositories/  Accès Postgres, seul endroit qui connaît SQL
  models/        Schémas et types partagés
  middlewares/   Authentification, journalisation, gestion d'erreurs
  config/        Chargement et validation des variables d'environnement
tests/
  integration/   Endpoints de bout en bout, base de test
  unit/          Services isolés, dépôts simulés
migrations/      Scripts versionnés de schéma
```

**Points d'attention**
- Seul `repositories/` importe le client Postgres : changer d'ORM ne touche qu'une couche.
- `services/` ne doit jamais manipuler `req` ni `res`, sans quoi il devient intestable hors HTTP.
