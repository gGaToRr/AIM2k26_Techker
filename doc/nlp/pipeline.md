# 📄 Documentation : Pipeline NLP (`nlp/pipeline.txt`)

## 📌 Rôle du Pipeline NLP
Le pipeline NLP est le cœur analytique de l'application. Il extrait le sens, la structure, le domaine et la complexité d'un prompt utilisateur **sans faire appel à une API externe**.

---

## 🔬 Les 8 Étapes Séquentielles

```text
Prompt Utilisateur Brut
          │
          ▼
[ Étape 1 : Nettoyage & Élisions ] ──────► Sanitizer.java
- Normalisation des apostrophes typographiques (’ -> ')
- Restauration des contractions (cest -> c'est, lavion -> l'avion)
- Préservation des symboles critiques (C++, C#, @, #)
          │
          ▼
[ Étape 2 : Lemmatisation & Découpage ] ─► Lemmatizer.java
- Découpage des identifiants CamelCase (getUserProfile -> get user profile)
- Filtrage exhaustif des stopwords (FR et EN)
- Détection des expressions multi-mots (pas-à-pas, root cause)
- Racinisation des formes verbales et pluriels
          │
          ▼
[ Étape 3 : Détection Stack Technique ] ─► TechStackDetector.java
- 12 familles technologiques (Java, Python, JS, C++, Rust, DevOps, Base de données...)
- Contexte académique et langues de traduction
          │
          ▼
[ Étape 4 : Extraction de Domaine ] ────► DomainExtractor.java
- Détection parmi 10 familles métiers (Gastronomie, Médecine, Musique, Droit...)
- Extraction du sujet pivot et assignation d'un persona expert
          │
          ▼
[ Étape 5 : Décomposition de Pensée ] ──► QuestionDecomposer.java
- Découpage des consignes en chaîne via connecteurs (puis, ensuite, aussi)
- Structuration en sous-objectifs ordonnés
          │
          ▼
[ Étape 6 : Métriques BPE & Coûts ] ────► TokenCounter.java
- Estimation des sous-mots BPE pour LLM
- Calcul prédictif du coût d'inférence (OpenAI, Claude, DeepSeek)
          │
          ▼
[ Étape 7 : Scoring de Qualité ] ───────► PromptQualityScorer.java
- Score global sur 100 (Clarté /40, Contexte /30, Contraintes /30)
- Points forts et pistes d'amélioration
          │
          ▼
[ Étape 8 : Classification Softmax ] ───► PromptClassifier.java
- Calcul des probabilités Softmax sur les 7 Archétypes Universels
- Détermination de l'archétype gagnant et niveau de confiance
          │
          ▼
    PromptProfile (Fiche d'Identité Sémantique Immuable)
```
