# 📄 Documentation : Pipeline de Génération (`genPrompt/pipeline.txt`)

## 📌 Rôle du Pipeline de Génération
Le pipeline de génération transforme les informations extraites par le NLP en un **Super-Prompt d'élite** formaté en Markdown pour les modèles de langage.

---

## 🏗️ Schéma de Résolution & Compilation Mustache

```text
                  PromptProfile (issu de l'analyse NLP)
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │      Routage vers le Sous-Template Fin         │
          │      (ex: "code_generation", "feynman", ...)    │
          │      ou Forçage par drapeau CLI (-t / --template│
          └─────────────────────────────────────────────────┘
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │           TemplateLoader.chargerTemplate()      │
          │      Lecture du fichier Markdown (.md)          │
          │      avec mise en cache mémoire (LRU)           │
          └─────────────────────────────────────────────────┘
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │       Construction du Contexte Mustache         │
          │  - cleanedMission : consigne nettoyée           │
          │  - domainPersona : persona d'expert métier      │
          │  - objectives : sous-objectifs décomposés       │
          │  - techStackSummary : technologies cibles       │
          │  - autoConstraints : règles d'auto-correction   │
          └─────────────────────────────────────────────────┘
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │             Rendu JMustache (Sans HTML)         │
          │  Compilation des variables {{tag}} et blocs     │
          │  ZÉRO résidu {{...}} non résolu                 │
          └─────────────────────────────────────────────────┘
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │         Adaptation Modèle / Agent IA            │
          │  Injection de balises <thinking>, XML ou format │
          │  pour Claude, DeepSeek, GPT-4o, Gemini, LLaMA-3 │
          └─────────────────────────────────────────────────┘
                                   │
                                   ▼
                      Prompt Optimisé Final (Markdown)
```
