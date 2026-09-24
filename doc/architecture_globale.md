# 🏛️ Architecture Globale du Projet

Ce document présente la vue d'ensemble du flux de traitement bout en bout (**End-to-End**) de l'application **Prompting Tool**.

---

## 🎯 Objectif du Projet
Le but de cet outil est de transformer n'importe quel prompt utilisateur brut, vague ou imparfait (souvent tapé à la va-vite) en un **Meta-Prompt d'élite**, structuré en Markdown, enrichi de directives d'experts, de personas adaptés et de contraintes méthodologiques prêtes pour les LLMs (GPT-4o, Claude 3.5, DeepSeek, Gemini, LLaMA-3), puis optionnellement d'**exécuter ce prompt directement sur un LLM léger local** (100% privé et hors-ligne).

---

## 🔄 Schéma du Pipeline Global

```text
               +-------------------------------------------+
               |     UTILISATEUR (CLI / Options / Pipe)    |
               +-------------------------------------------+
                                     │
                                     ▼
                      [ 1. Saisie & Parsing CLI ]
               - Arguments courts/longs (-h, -v, -i, -c, -e, -m, -V...)
               - Détection mode interactif / direct
               - Lecture de fichiers éventuels (-f, -c)
                                     │
                                     ▼
                   [ 2. Analyse Préventive de Sécurité ]
               - SafetyAdvisor : détection de termes sensibles
               - Avertissement non bloquant si nécessaire
                                     │
                                     ▼
                    [ 3. Pipeline d'Analyse NLP ]
               ┌───────────────────────────────────────────┐
               │ a. Sanitizer : restauration des élisions  │
               │ b. Lemmatizer : tokenisation & stopwords  │
               │ c. TechStackDetector : technos & langues  │
               │ d. DomainExtractor : persona & sujet      │
               │ e. QuestionDecomposer : sous-objectifs    │
               │ f. TokenCounter : BPE & estimation coûts  │
               │ g. PromptQualityScorer : score /100       │
               │ h. PromptClassifier : Softmax 7 Archétypes│
               └───────────────────────────────────────────┘
                                     │
                                     ▼
                      [ 4. Fiche Sémantique Immuable ]
                               PromptProfile
                                     │
                                     ▼
                     [ 5. Moteur Meta-Prompting ]
               - MetaPromptEngine & TemplateLoader
               - Résolution de sous-template (18 templates)
               - Compilation JMustache (sans résidu {{...}})
               - Adaptation ciblée pour l'IA (-a claude/gpt/deepseek...)
                                     │
                                     ▼
                       [ 6. Sortie & Restitution ]
               - Affichage console standard
               - Rapport verbeux détaillé (-V)
               - Sortie brute pour pipe Unix (-r)
               - Formatage / Export JSON (-o json / -o fichier.json)
               - Export fichier Markdown (-o fichier.md)
               - Copie presse-papiers (-C)
                                     │
                                     ▼ (si -e ou --exec activé)
               [ 7. Module Inférence Locale & LLM Expert ]
               ┌───────────────────────────────────────────┐
               │ a. ModelRouter : routage sémantique       │
               │ b. ModelInstaller : onboarding noob & DL  │
               │ c. LocalLlmBackend : inférence native     │
               │ d. Streaming console & métriques (TPS)    │
               └───────────────────────────────────────────┘
```

---

## 🧩 Les 5 Grands Modules du Projet

1. **Module `menu` & `cli`** : Gère les interactions utilisateur, la saisie multiligne robuste et le parsing exhaustif de tous les drapeaux de commande.
2. **Module `nlp`** : Analyse sémantiquement le texte sans intelligence artificielle externe (100% algorithmique locale déterministe).
3. **Module `gen` & `genPrompt`** : Contient le moteur de templating et les 18 matrices de prompts classées selon les 7 Archétypes Universels.
4. **Module `llm`** : Routeur intelligent et moteur d'inférence locale pour exécuter le prompt sur des modèles experts légers (Qwen, Gemma, DeepSeek, SmolLM).
5. **Module `test`** : Suite de tests automatisés TDD (129 tests) garantissant une fiabilité totale et une régression zéro.
