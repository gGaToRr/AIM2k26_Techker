# 📄 Documentation : `src/nlp/PromptClassifier.java`

## 📌 Rôle du Fichier
`src/nlp/PromptClassifier.java` est le **classifieur probabiliste** du système. Il évalue les scores sémantiques de chaque catégorie et applique une fonction **Softmax** pour générer une distribution de probabilité sommée à 100% sur les 7 Archétypes Universels.

---

## ⚙️ Algorithme de Scoring & Softmax

1. **Calcul des Scores Bruts** :
   - Chaque archétype reçoit une contribution basée sur les lemmes pondérés (`profile.weightedScores()`), la présence de code, les technologies détectées et les motifs de phrase.

2. **Normalisation Softmax** :
   Pour chaque score brut $z_i$, la probabilité $P_i$ est calculée par :
   $$P_i = \frac{e^{z_i / T}}{\sum_{j=1}^{7} e^{z_j / T}}$$
   *(où $T$ est la température de lissage)*.

3. **Niveau de Confiance** :
   - `HIGH` : La probabilité de l'archétype vainqueur dépasse 55%.
   - `MEDIUM` : La probabilité est comprise entre 35% et 55%.
   - `LOW` : La probabilité est inférieure à 35% (ambiguïté sémantique).

---

## 📋 Structure du Résultat (`ClassificationResult`)

```java
public record ClassificationResult(
        TypeOfPrompt primaryType,                  // Archétype vainqueur
        double primaryProbability,                 // Probabilité en pourcentage (ex: 82.5%)
        ConfidenceLevel confidenceLevel,           // HIGH, MEDIUM, LOW
        Map<TypeOfPrompt, Double> distribution,    // Répartition complète sur les 7 archétypes
        String justification                       // Explication de la décision
)
```

---

## 👨‍💻 Exemple d'Utilisation

```java
ClassificationResult resultat = PromptClassifier.classifier(profile, techStack, langueCible);
System.out.println("Archétype gagnant : " + resultat.primaryType());
System.out.println("Probabilité : " + resultat.primaryProbability() + "%");
System.out.println("Niveau de confiance : " + resultat.confidenceLevel());
```
