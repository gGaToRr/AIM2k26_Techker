# 📄 Documentation : `src/nlp/TokenCounter.java`

## 📌 Rôle du Fichier
`src/nlp/TokenCounter.java` estime le nombre de sous-mots **BPE (Byte-Pair Encoding)** du prompt et calcule une estimation prédictive des coûts d'inférence sur les APIs LLM.

---

## ⚙️ Mécanisme d'Estimation BPE

1. **Regex de Sous-Mots** :
   - Découpe le texte en blocs selon les conventions de tokenisation modernes :
     ```regexp
     (?i)'[a-z]+|[\p{L}]+|[\p{N}]+|[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+
     ```
2. **Gestion des Mots Longs** :
   - Les mots de plus de 6 caractères sont divisés en plusieurs sous-tokens ($\lceil \text{longueur} / 4.0 \rceil$).

3. **Modèles de Tarification Référencés** :
   - OpenAI GPT-4o : ~$2.50 pour 1 million de tokens d'entrée.
   - Claude 3.5 Sonnet : ~$3.00 pour 1 million de tokens.
   - DeepSeek-V3 : ~$0.14 pour 1 million de tokens.

---

## 📋 Structure du Résultat (`TokenMetrics`)

```java
public record TokenMetrics(
        int estimatedTokens,          // Nombre estimé de tokens BPE
        int characterCount,           // Nombre total de caractères
        int wordCount,                // Nombre de mots
        double estimatedCostDollars,  // Coût moyen estimé en dollars
        String contextWindowFitness   // Taux d'occupation d'une fenêtre de 128k (ex: 0.02%)
)
```

---

## 👨‍💻 Exemple d'Utilisation

```java
TokenMetrics metrics = TokenCounter.analyser("Explique les arbres B+ et le tri par tas");
System.out.println("Tokens BPE : " + metrics.estimatedTokens());
System.out.println("Coût estimé : $" + metrics.estimatedCostDollars());
```
