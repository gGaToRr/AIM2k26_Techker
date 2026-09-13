# 📄 Documentation : `nlp/QuestionDecomposer.java`

## 📌 Rôle du Fichier
`nlp/QuestionDecomposer.java` décompose les requêtes complexes comportant plusieurs consignes en une séquence ordonnée de **sous-objectifs clairs**.

---

## ⚙️ Fonctionnement

1. **Détection des Connecteurs Séquentiels** :
   - Détecte les connecteurs logiques de succession : `puis`, `ensuite`, `après`, `et aussi`, `de plus`, `enfin`, `également`.

2. **Scission & Nettoyage** :
   - Découpe le flux de pensée de l'utilisateur en segments indépendants.
   - Nettoie les connecteurs de liaison pour ne conserver que la mission de chaque segment.

3. **Formatage XML Structuré** :
   - Produit une liste formatée en balises XML `<objectifs_specifiques>` prête à être injectée dans les templates de prompt :
     ```xml
     <objectifs_specifiques>
     - Étape 1 : Analyser le code
     - Étape 2 : Proposer un correctif
     </objectifs_specifiques>
     ```

---

## 📋 Structure du Résultat (`DecompositionResult`)

```java
public record DecompositionResult(
        List<String> objectives,         // Liste des sous-objectifs découpés
        String xmlFormattedList,         // Bloc formaté en XML Markdown
        boolean hasMultipleObjectives    // True si au moins 2 sous-objectifs distincts
)
```

---

## 👨‍💻 Exemple d'Utilisation

```java
DecompositionResult res = QuestionDecomposer.decomposer("Déboguer ce script Python puis optimiser les requêtes SQL");
System.out.println("Multi-objectifs : " + res.hasMultipleObjectives()); // true
System.out.println("Objectifs : " + res.objectives()); // ["Déboguer ce script Python", "Optimiser les requêtes SQL"]
```
