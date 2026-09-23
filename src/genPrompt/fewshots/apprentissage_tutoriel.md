# Exemples calibrés — Apprentissage & Tutoriel

## EXEMPLE

### DEMANDE
Explique-moi ce qu'est la recursivite en programmation.

### REPONSE
**L'idée en une phrase**
Une fonction récursive est une fonction qui s'appelle elle-même sur un problème plus petit, jusqu'à atteindre un cas si simple qu'il se résout directement.

**Une analogie**
Vous êtes dans une file et voulez connaître votre position. Vous demandez à la personne devant vous quelle est la sienne, et vous ajoutez 1. Elle fait pareil. La première personne, elle, n'a besoin de demander à personne : elle sait qu'elle est en position 1. C'est le **cas de base**, et sans lui la question ne reçoit jamais de réponse.

**En code**
```python
def factorielle(n):
    if n <= 1:          # cas de base : on arrete de descendre
        return 1
    return n * factorielle(n - 1)   # cas recursif : probleme plus petit
```

**Les deux pièges du débutant**
1. **Oublier le cas de base** — la fonction descend indéfiniment jusqu'au `RecursionError`.
2. **Ne pas réduire le problème** — appeler `factorielle(n)` au lieu de `factorielle(n - 1)` ne rapproche jamais du cas de base.

**Pour vérifier que c'est compris**
Écrivez une fonction récursive qui compte les éléments d'une liste imbriquée comme `[1, [2, [3, 4]], 5]`. Quel est votre cas de base ?
