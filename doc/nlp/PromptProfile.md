# 📄 Documentation : `nlp/PromptProfile.java`

## 📌 Rôle du Fichier
`nlp/PromptProfile.java` est le **Record Java immuable** servant de pivot de données entre la phase d'analyse NLP et la phase de méta-génération de prompt.

---

## 📋 Structure des Données

```java
public record PromptProfile(
        String rawText,                                         // Texte brut d'origine
        String sanitizedText,                                   // Texte nettoyé avec élisions restaurées
        List<String> tokens,                                    // Liste ordonnée des tokens nettoyés
        Map<String, Integer> lemmaFrequencies,                  // Fréquence d'apparition de chaque lemme
        Map<String, Double> weightedScores,                     // Poids sémantiques associés aux lemmes
        double codeDensity,                                     // Pourcentage de symboles et mots-clés de code (0.0 à 1.0)
        boolean isQuestion,                                     // Indique si le prompt contient une question (?)
        boolean isCommand,                                      // Indique si le prompt commence par un verbe d'action
        String language,                                        // Langue détectée ("FR" ou "EN")
        List<String> detectedTechnologies,                      // Liste des technos reconnues (Java, SQL, Docker...)
        Optional<String> targetTranslationLanguage,             // Langue cible si demande de traduction
        DomainExtractor.DomainInfo domainInfo,                  // Domaine métier et persona expert assigné
        QuestionDecomposer.DecompositionResult decomposition,   // Décomposition des sous-objectifs ordonnés
        TokenCounter.TokenMetrics tokenMetrics,                 // Métriques BPE et estimation des coûts API
        PromptQualityScorer.Diagnostic qualityDiagnostic,       // Score de qualité /100 et pistes d'amélioration
        PromptClassifier.ClassificationResult classification    // Résultat Softmax des 7 Archétypes Universels
)
```

---

## 🛠️ Méthodes Utilitaires

* `hasCode()` : Retourne `true` si la densité de code dépasse le seuil critique de `0.15` (15%).
