# 📄 Documentation : `nlp/PromptQualityScorer.java`

## 📌 Rôle du Fichier
`nlp/PromptQualityScorer.java` analyse la clarté et l'exhaustivité du prompt de l'utilisateur pour lui attribuer une note de **qualité globale sur 100 points**, tout en fournissant des pistes d'amélioration concrètes.

---

## 📊 Grille d'Évaluation (100 Points)

1. **Clarté de la Consigne (40 points)** :
   - Présence d'un verbe d'action clair (ex: *créer*, *expliquer*, *analyser*) ou d'une question explicite : **+25 pts**.
   - Longueur équilibrée (entre 8 et 200 mots) : **+15 pts** (trop court : +5 pts, trop long : +5 pts).

2. **Richesse du Contexte (30 points)** :
   - Présence de termes techniques, noms de fichiers ou technologies cibles : **+15 pts**.
   - Présence de code source ou snippet structuré : **+15 pts**.

3. **Précision des Contraintes (30 points)** :
   - Présence de contraintes de formatage ou structure (*markdown*, *json*, *tableau*, *liste*) : **+15 pts**.
   - Présence de contraintes de périmètre (*étape par étape*, *sans code*, *pour débutant*) : **+15 pts**.

---

## 📋 Structure du Résultat (`Diagnostic`)

```java
public record Diagnostic(
        int scoreGlobal,                     // Score global sur 100
        int scoreClarte,                      // Note de clarté sur 40
        int scoreContexte,                    // Note de contexte sur 30
        int scoreContraintes,                 // Note de contraintes sur 30
        List<String> pointsForts,             // Points forts identifiés
        List<String> pistesAmelioration       // Conseils d'optimisation
)
```

---

## 👨‍💻 Exemple d'Utilisation

```java
Diagnostic diag = PromptQualityScorer.evaluer("Créer un tableau comparatif entre Java et Rust en Markdown étape par étape", false, true, false);
System.out.println("Score : " + diag.scoreGlobal() + "/100");
System.out.println("Points forts : " + diag.pointsForts());
```
