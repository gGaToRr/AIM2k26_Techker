# 📄 Documentation : Répertoire des 129 Tests TDD (`test/`)

Ce document présente l'inventaire complet des 129 tests automatisés garantissant la robustesse du projet.

---

## 📊 Répartition des 23 Classes de Tests

### 1. 🔬 Tests du Module NLP (`test/nlp/`)
* **`SanitizerTest` (9 tests)** : Nettoyage, élisions verbales/articles/prépositions, apostrophes typographiques, symboles C++/C#, capitalisation, cas limites vides/nulls.
* **`LemmatizerTest` (9 tests)** : Tokenisation, stopwords FR/EN, découpage CamelCase, expressions multi-mots, racinisation, densité de code, détection de langue et intégrité de profil.
* **`TechStackDetectorTest` (4 tests)** : Couverture des 12 familles technologiques, détection des langues cibles (`vers l'anglais`, `vers le japonais`), contextes académiques (42, Epitech) et entrées sans technos.
* **`DomainExtractorTest` (4 tests)** : Les 10 familles de domaines métiers, extraction du sujet pivot dynamique, fallback généraliste et forçage de persona.
* **`QuestionDecomposerTest` (4 tests)** : Découpage de flux complexes via connecteurs (`puis`, `ensuite`, `aussi`), formatage XML et requêtes simples sans découpage.
* **`TokenCounterTest` (3 tests)** : Estimation des sous-mots BPE, calcul des coûts API pour OpenAI/Claude/DeepSeek et entrées vides.
* **`PromptQualityScorerTest` (3 tests)** : Validation des intervalles de score (0-100), impact du code/contraintes et prompt vide.
* **`SafetyAdvisorTest` (4 tests)** : Détection des termes sensibles, attaques et jailbreaks avec avertissement préventif.
* **`PromptClassifierTest` (8 tests)** : Classification probabiliste des 7 archétypes universels et normalisation de la distribution Softmax (somme à 100%).

### 2. 📝 Tests du Module GenPrompt (`test/gen/`)
* **`TemplateLoaderTest` (8 tests)** : Chargement de templates sur l'ensemble des 7 archétypes, mise en cache mémoire et fallback sur template générique.
* **`MetaPromptEngineTest` (4 tests)** : Rendu sans résidu `{{...}}`, conservation du code et caractères spéciaux, injection dynamique de personas et ciblages LLMs.
* **`TemplateSyntaxValidationTest` (1 test)** : Parser automatisé inspectant l'intégrité syntaxique et l'équilibre des blocs `{{#tag}}` / `{{/tag}}` sur les 18 fichiers `.md`.

### 3. 💻 Tests du Module CLI & Arguments (`test/cli/` & `test/menu/`)
* **`MenuTest` (4 tests)** : Saisie multiligne, sentinelles `:done` / `:END`, collage de blocs de code indentés et deux-points internes.
* **`CliParserTest` (14 tests)** : Couverture individuelle de chacun des arguments CLI (`-h`, `-v`, `-V`, `-i`, `-c`, `-e`, `-m`, `-a`, `-o`, `-t`, `-d`, `-l`, `-r`, `-f`, `-n`, `-C`).
* **`CliSyntaxParsingTest` (5 tests)** : Syntaxes courtes/longues, syntaxes `--flag=valeur` et `-f=valeur`, ordre arbitraire des arguments et arguments positionnels.
* **`CliOutputAndExportTest` (5 tests)** : Structure du schéma JSON, échappement des caractères spéciaux, exports fichiers Markdown/JSON et méthode presse-papiers.
* **`CliSemanticOverridesTest` (4 tests)** : Surcharges de templates, forçage de domaines, langues cibles (FR, EN, ES, DE) et formatage pour les 6 LLMs cibles.
* **`CliRobustnessAndEdgeCasesTest` (5 tests)** : Fichiers inexistants avec fallback sans crash, valeurs manquantes en fin de commande, espaces multiples et préservation XML.

### 4. 🤖 Tests du Module LLM & Inférence Locale (`test/llm/`)
* **`ModelTypeTest` (4 tests)** : Intégrité des 4 modèles légers, métadonnées, URLs HuggingFace, correspondances d'archétypes et résolution d'alias tolérants.
* **`ModelRouterTest` (7 tests)** : Prise de décision de routage sur code, debug, comparatif complexe, questions directes courtes, culture générale et overrides manuels.
* **`LlmConfigTest` (4 tests)** : Valeurs par défaut, sérialisation/désérialisation JSON, persistance disque et résilience face aux JSONs corrompus.
* **`ModelInstallerTest` (5 tests)** : Détection d'installation locale, validation du contenu pédagogique de l'onboarding débutant et scénarios d'interaction utilisateur (accord, sélection, refus).
* **`LlmEngineTest` (4 tests)** : Inférence locale autonome et native, streaming console, calcul des métriques de débit (tokens/sec) et refus d'autorisation.

### 5. 🌐 Tests d'Intégration Bout en Bout (`test/integration/`)
* **`EndToEndIntegrationTest` (7 tests)** : 7 scénarios réels de bout en bout (Génie logiciel, Gastronomie, Feynman, Débogage, Musique, Comparaison de bases de données, Traduction technique).

---

## 🚀 Exécution de la Suite Globale
```bash
./scripts/run_tests.sh
```
*Temps moyen d'exécution : ~300 millisecondes (129/129 succès).*
