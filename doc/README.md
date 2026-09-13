# 📚 Documentation Technique Complète — Prompting Tool (AIM2k26)

Bienvenue dans la documentation officielle du projet **Prompting Tool (AIM2k26)**.

Ce dossier `doc/` a été conçu pour permettre à **n'importe quel développeur** (débutant ou confirmé) de comprendre immédiatement le fonctionnement, l'architecture et les détails d'implémentation de chaque fichier du projet.

---

## 🗺️ Sommaire de la Documentation

### 1. Architecture Globale & Point d'Entrée
* 📄 [`doc/architecture_globale.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/architecture_globale.md) : Vue d'ensemble du flux end-to-end (saisie → NLP → Meta-Prompting → sortie).
* 📄 [`doc/Main.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/Main.md) : Orchestration et point d'entrée principal (`Main.java`).
* 📄 [`doc/menu/Menu.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/menu/Menu.md) : Interface utilisateur CLI interactive et saisie multiligne (`menu/Menu.java`).

### 2. Module CLI & Gestion des Arguments (`cli/`)
* 📄 [`doc/cli/CliArgs.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/cli/CliArgs.md) : Structure de données immuable des options (`cli/CliArgs.java`).
* 📄 [`doc/cli/CliParser.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/cli/CliParser.md) : Parser d'arguments, manuel d'aide et rapport verbeux (`cli/CliParser.java`).
* 📄 [`doc/cli/CliClipboard.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/cli/CliClipboard.md) : Gestionnaire presse-papiers multi-plateforme (`cli/CliClipboard.java`).

### 3. Module NLP — Analyse & Traitement du Langage (`nlp/`)
* 📄 [`doc/nlp/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/pipeline.md) : Schéma et phases du pipeline NLP (`nlp/pipeline.txt`).
* 📄 [`doc/nlp/Sanitzer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/Sanitzer.md) : Nettoyage, restauration des élisions et contractions (`nlp/Sanitzer.java`).
* 📄 [`doc/nlp/Lemmatizer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/Lemmatizer.md) : Tokenisation, stopwords et racinisation (`nlp/Lemmatizer.java`).
* 📄 [`doc/nlp/PromptProfile.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/PromptProfile.md) : Fiche d'identité sémantique et métriques (`nlp/PromptProfile.java`).
* 📄 [`doc/nlp/TypeOfPrompt.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/TypeOfPrompt.md) : Les 7 Archétypes Universels d'Intention (`nlp/TypeOfPrompt.java`).
* 📄 [`doc/nlp/PromptClassifier.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/PromptClassifier.md) : Classification probabiliste Softmax (`nlp/PromptClassifier.java`).
* 📄 [`doc/nlp/TechStackDetector.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/TechStackDetector.md) : Détection de technologies et contextes (`nlp/TechStackDetector.java`).
* 📄 [`doc/nlp/DomainExtractor.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/DomainExtractor.md) : Extraction de domaine métier et persona expert (`nlp/DomainExtractor.java`).
* 📄 [`doc/nlp/QuestionDecomposer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/QuestionDecomposer.md) : Décomposition en sous-objectifs (`nlp/QuestionDecomposer.java`).
* 📄 [`doc/nlp/TokenCounter.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/TokenCounter.md) : Estimation tokens BPE et coûts d'inférence (`nlp/TokenCounter.java`).
* 📄 [`doc/nlp/PromptQualityScorer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/PromptQualityScorer.md) : Évaluation de la qualité sur 100 (`nlp/PromptQualityScorer.java`).
* 📄 [`doc/nlp/SafetyAdvisor.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/SafetyAdvisor.md) : Détection préventive de termes sensibles (`nlp/SafetyAdvisor.java`).

### 4. Module GenPrompt — Meta-Prompting & Templates (`gen/` & `genPrompt/`)
* 📄 [`doc/gen/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/pipeline.md) : Architecture de rendu et injection contextuelle (`genPrompt/pipeline.txt`).
* 📄 [`doc/gen/MetaPromptEngine.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/MetaPromptEngine.md) : Moteur d'optimisation et formatage LLM (`gen/MetaPromptEngine.java`).
* 📄 [`doc/gen/TemplateLoader.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/TemplateLoader.md) : Chargeur et cache mémoire des templates (`gen/TemplateLoader.java`).
* 📄 [`doc/gen/templates.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/templates.md) : Guide des 18 templates Markdown spécialisés.

### 5. Suite de Tests TDD & Automatisation (`test/` & `scripts/`)
* 📄 [`doc/test/framework.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/test/framework.md) : Mini-framework d'assertions et runner de test (`test/framework/`).
* 📄 [`doc/test/test_suites.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/test/test_suites.md) : Vue d'ensemble des 101 tests unitaires et d'intégration.
* 📄 [`doc/scripts/run_tests.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/scripts/run_tests.md) : Script d'exécution automatisée (`scripts/run_tests.sh`).

---

## 💡 Principes Clés du Projet
1. **Zéro dépendance lourde** : Utilise uniquement la bibliothèque standard Java + `JMustache` (45 KB) pour le rendu Markdown.
2. **Exécution ultra-rapide** : Analyse NLP + génération de super-prompt en **moins de 5 millisecondes**.
3. **100% Test-Driven (TDD)** : 101 tests garantissant la non-régression et la robustesse face aux cas limites.
