# 🗺️ Répertoire des Schémas de Pipelines (`doc/pipelines/`)

Ce dossier regroupe tous les schémas textuels ASCII et diagrammes d'architecture des différents pipelines du projet.

---

## 📂 Fichiers de Pipeline Disponibles

| Fichier | Périmètre & Description | Composants Clés |
|---|---|---|
| [**`pipeline_global.txt`**](file:///home/kaets0ner/Desktop/Project_Ia/doc/pipelines/pipeline_global.txt) | **Pipeline Global Bout-en-Bout** : de la saisie utilisateur (CLI/Menu) jusqu'à la sortie ou l'inférence locale. | [`Main.java`](file:///home/kaets0ner/Desktop/Project_Ia/Main.java), [`src/cli/`](file:///home/kaets0ner/Desktop/Project_Ia/src/cli), [`src/nlp/`](file:///home/kaets0ner/Desktop/Project_Ia/src/nlp), [`src/gen/`](file:///home/kaets0ner/Desktop/Project_Ia/src/gen), [`src/llm/`](file:///home/kaets0ner/Desktop/Project_Ia/src/llm) |
| [**`pipeline_nlp.txt`**](file:///home/kaets0ner/Desktop/Project_Ia/doc/pipelines/pipeline_nlp.txt) | **Pipeline d'Analyse NLP & Sémantique** : nettoyage, tokenisation, détection de stack, scoring et classification. | [`src/nlp/Sanitzer.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/nlp/Sanitzer.java), [`src/nlp/Lemmatizer.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/nlp/Lemmatizer.java), [`src/nlp/PromptClassifier.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/nlp/PromptClassifier.java) |
| [**`pipeline_genprompt.txt`**](file:///home/kaets0ner/Desktop/Project_Ia/doc/pipelines/pipeline_genprompt.txt) | **Pipeline de Méta-Génération** : chargement des matrices Mustache et injection contextuelle dynamique. | [`src/gen/TemplateLoader.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/gen/TemplateLoader.java), [`src/gen/MetaPromptEngine.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/gen/MetaPromptEngine.java) |
| [**`pipeline_llm.txt`**](file:///home/kaets0ner/Desktop/Project_Ia/doc/pipelines/pipeline_llm.txt) | **Pipeline d'Inférence Embarquée & Routage** : sélection intelligente de SLM expert, onboarding et streaming local. | [`src/llm/ModelRouter.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/llm/ModelRouter.java), [`src/llm/ModelInstaller.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/llm/ModelInstaller.java), [`src/llm/LlmEngine.java`](file:///home/kaets0ner/Desktop/Project_Ia/src/llm/LlmEngine.java) |

---

## 🔗 Liens avec la Documentation Détaillée

- 📄 Schéma NLP : [`doc/nlp/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/nlp/pipeline.md)
- 📄 Schéma GenPrompt : [`doc/gen/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/gen/pipeline.md)
- 📄 Schéma LLM : [`doc/llm/pipeline.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/pipeline.md)
- 📄 Architecture Globale : [`doc/architecture_globale.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/architecture_globale.md)
