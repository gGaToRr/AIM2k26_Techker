# 📚 Documentation Technique Complète — Prompting Tool (AIM2k26)

Bienvenue dans la documentation officielle du projet **Prompting Tool (AIM2k26)**.

Ce dossier `doc/` a été conçu pour permettre à **n'importe quel développeur** (débutant ou confirmé) de comprendre immédiatement le fonctionnement, l'architecture et les détails d'implémentation de chaque fichier du projet.

---

## 🗺️ Sommaire de la Documentation

### 1. Architecture Globale, Point d'Entrée & Pipelines
* 📄 [`doc/architecture_globale.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/architecture_globale.md) : Vue d'ensemble du flux end-to-end (saisie → NLP → Meta-Prompting → Inférence Locale → sortie).
* 📄 [`doc/pipelines/README.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/pipelines/README.md) : Répertoire des schémas textuels de pipelines (`doc/pipelines/`).
* 📄 [`doc/Main.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/Main.md) : Orchestration et point d'entrée principal (`Main.java`).
* 📄 [`doc/menu/Menu.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/menu/Menu.md) : Interface utilisateur CLI interactive et saisie multiligne (`src/menu/Menu.java`).

### 2. Module CLI & Gestion des Arguments (`src/cli/`)
* 📄 [`doc/cli/CliArgs.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/cli/CliArgs.md) : Structure de données immuable des options (`src/cli/CliArgs.java`).
* 📄 [`doc/cli/CliParser.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/cli/CliParser.md) : Parser d'arguments, manuel d'aide et rapport verbeux (`src/cli/CliParser.java`).
* 📄 [`doc/cli/CliClipboard.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/cli/CliClipboard.md) : Gestionnaire presse-papiers multi-plateforme (`src/cli/CliClipboard.java`).

### 3. Module NLP — Analyse & Traitement du Langage (`src/nlp/`)
* 📄 [`doc/nlp/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/pipeline.md) : Schéma et phases du pipeline NLP (`doc/pipelines/pipeline_nlp.txt`).
* 📄 [`doc/nlp/Sanitzer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/Sanitzer.md) : Nettoyage, restauration des élisions et contractions (`src/nlp/Sanitzer.java`).
* 📄 [`doc/nlp/Lemmatizer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/Lemmatizer.md) : Tokenisation, stopwords et racinisation (`src/nlp/Lemmatizer.java`).
* 📄 [`doc/nlp/PromptProfile.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/PromptProfile.md) : Fiche d'identité sémantique et métriques (`src/nlp/PromptProfile.java`).
* 📄 [`doc/nlp/TypeOfPrompt.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/TypeOfPrompt.md) : Les 7 Archétypes Universels d'Intention (`src/nlp/TypeOfPrompt.java`).
* 📄 [`doc/nlp/PromptClassifier.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/PromptClassifier.md) : Classification probabiliste Softmax (`src/nlp/PromptClassifier.java`).
* 📄 [`doc/nlp/TechStackDetector.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/TechStackDetector.md) : Détection de technologies et contextes (`src/nlp/TechStackDetector.java`).
* 📄 [`doc/nlp/DomainExtractor.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/DomainExtractor.md) : Extraction de domaine métier et persona expert (`src/nlp/DomainExtractor.java`).
* 📄 [`doc/nlp/QuestionDecomposer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/QuestionDecomposer.md) : Décomposition en sous-objectifs (`src/nlp/QuestionDecomposer.java`).
* 📄 [`doc/nlp/TokenCounter.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/TokenCounter.md) : Estimation tokens BPE et coûts d'inférence (`src/nlp/TokenCounter.java`).
* 📄 [`doc/nlp/PromptQualityScorer.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/PromptQualityScorer.md) : Évaluation de la qualité sur 100 (`src/nlp/PromptQualityScorer.java`).
* 📄 [`doc/nlp/SafetyAdvisor.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/SafetyAdvisor.md) : Détection préventive de termes sensibles (`src/nlp/SafetyAdvisor.java`).

### 4. Module GenPrompt — Meta-Prompting & Templates (`src/gen/` & `genPrompt/`)
* 📄 [`doc/gen/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/pipeline.md) : Architecture de rendu et injection contextuelle (`doc/pipelines/pipeline_genprompt.txt`).
* 📄 [`doc/gen/MetaPromptEngine.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/MetaPromptEngine.md) : Moteur d'optimisation et formatage LLM (`src/gen/MetaPromptEngine.java`).
* 📄 [`doc/gen/TemplateLoader.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/TemplateLoader.md) : Chargeur et cache mémoire des templates (`src/gen/TemplateLoader.java`).
* 📄 [`doc/gen/templates.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/templates.md) : Guide des 18 templates Markdown spécialisés.

### 5. Module LLM — Inférence Locale & Routage Intelligent (`src/llm/`)
* 📄 [`doc/llm/README.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/README.md) : Présentation générale de l'inférence locale privée.
* 📄 [`doc/llm/ModelType.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelType.md) : Registre des 4 modèles légers (Qwen Coder, Gemma 2, DeepSeek R1, SmolLM2).
* 📄 [`doc/llm/ModelRouter.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelRouter.md) : Routage automatique sémantique selon le profil NLP.
* 📄 [`doc/llm/ModelInstaller.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelInstaller.md) : Assistant d'onboarding pour débutants et gestionnaire de téléchargement.
* 📄 [`doc/llm/LlmConfig.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/LlmConfig.md) : Gestion de la configuration persistante (`settings.json`).
* 📄 [`doc/llm/LlmEngine.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/LlmEngine.md) : Moteur d'inférence, streaming console et métriques de débit.
* 📄 [`doc/llm/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/pipeline.md) : Schéma et phases du pipeline LLM (`doc/pipelines/pipeline_llm.txt`).

### 6. Suite de Tests TDD & Automatisation (`test/` & `scripts/`)
* 📄 [`doc/test/framework.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/test/framework.md) : Mini-framework d'assertions et runner de test (`test/framework/`).
* 📄 [`doc/test/test_suites.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/test/test_suites.md) : Vue d'ensemble des 129 tests unitaires et d'intégration.
* 📄 [`doc/scripts/run_tests.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/scripts/run_tests.md) : Script d'exécution automatisée (`scripts/run_tests.sh`).

---

## 💡 Principes Clés du Projet
1. **Zéro dépendance lourde** : Utilise uniquement la bibliothèque standard Java + `JMustache` (45 KB) pour le rendu Markdown.
2. **Exécution ultra-rapide** : Analyse NLP + génération de super-prompt en **moins de 5 millisecondes**.
3. **Inférence 100% Locale & Privée** : Possibilité d'exécuter localement le prompt généré sur des SLMs spécialisés sans envoyer de données sur le cloud.
4. **100% Test-Driven (TDD)** : 129 tests unitaires et d'intégration garantissant la robustesse face aux cas limites.
