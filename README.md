# Prompting_Tool (AIM2k26_Techker)
Vous en avez marre des réponses vagues ou incomplètes des LLMs ? Le moteur **[Prompting Tool]** est un optimiseur de requêtes open-source : il analyse votre consigne brute via un pipeline NLP et génère un Super-Prompt ultra-structuré (Meta-Prompting).

<div align="center">

<img src="doc/images/banniere.svg" alt="AIM2K26_Prompting" width="100%" />
<br />

![Status](https://img.shields.io/badge/status-Working-green?style=flat-square)
![Type](https://img.shields.io/badge/type-open%20source-purple?style=flat-square)
![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Offline](https://img.shields.io/badge/100%25-Offline-blue?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

<br />

</div>

---

## # Aperçu

```text
=========================================
   Détecteur et Améliorateur de Prompt   
=========================================

Entrez votre prompt : cree moi une fonction python pour trier un tableau

--- [1. ANALYSE DU PROMPT] ---
Nature détectée : CODE
Confiance       : 97.0% (HIGH)
Langue          : FR
Technologies    : [Python]
Tokens estimés  : 19
Score qualité   : 80/100

--- [2. PROMPT OPTIMISÉ POUR LE LLM] ---

# RÔLE & EXPERTISE
Tu es un Architecte Logiciel Senior et Développeur Expert de rang mondial spécialisé en Python.
Tu rédiges exclusivement du code de niveau "Production-Ready", propre, testable, sécurisé et performant.

<contexte_technique>
- Technologies cibles : Python
- Langue de réponse : Français
- Niveau de certitude de détection : HIGH (97.0%)
</contexte_technique>

<instruction_utilisateur>
cree moi une fonction python pour trier un tableau
</instruction_utilisateur>

<regles_de_developpement_strictes>
1. **Architecture & SOLID** : Respecter la séparation stricte des responsabilités et les principes SOLID.
2. **Typage Strict & Validation** : Typage explicite sans ambiguïté. Valider les entrées et gérer les cas limites.
3. **Gestion des Erreurs Robuste** : Utiliser des exceptions typées, logging structuré et ne jamais avaler silencieusement les erreurs.
4. **Zéro Placeholder** : Écrire l'implémentation complète sans `// TODO` ou `...`.
</regles_de_developpement_strictes>

<processus_de_pensee_cot>
Avant d'écrire le code, effectue une brève analyse (2-3 phrases) :
1. Découpage du problème et choix d'architecture.
2. Identification des cas limites.
</processus_de_pensee_cot>

<format_de_sortie>
- **1. Analyse & Décisions** : Justification rapide de la structure choisie.
- **2. Code Source Complet** : Code prêt pour la production avec le nom du fichier en entête.
- **3. Test / Démo d'Utilisation** : Exemple d'appel concret ou test unitaire confirmant le bon fonctionnement.
</format_de_sortie>
```

<br />

## # Fonctionnalités

|      | Module | Description |
| :--: | ------ | ----------- |
|  🛡️  | **Safety Advisor** | Détecte préventivement les termes sensibles risquant de déclencher les filtres LLM |
|  🧹  | **Sanitizer & NLP** | Nettoyage ponctuation, suppression accents, StopWords et lemmatisation |
|  🔬  | **Tech Stack** | Détection auto des langages (Python, C++, Java, JS...), frameworks et contextes (42, Epitech) |
|  📊  | **Scoring Qualité** | Note /100 basée sur la clarté de la consigne, le contexte et les contraintes |
|  🪙  | **Token Counter** | Estimation BPE des sous-mots (tokens), coût ($) et empreinte fenêtre 128K |
|  🎯  | **Classifier** | Détection d'intention (Code, Traduction, Correction, Création, Questions) |
|  🚀  | **Meta-Prompting** | Retrait du bruit conversationnel, persona expert et compilation Mustache |

<br />

## # Installation & Utilisation

**Prérequis** : un JDK 21+ (`javac -version`). Maven est optionnel — sans lui, la bibliothèque JMustache reste disponible dans `src/lib/`.

Clonez la dernière version publiée (`--branch v1.0.1`) : la branche par défaut du dépôt est une branche de développement.

<br />

### Option A — Linux / macOS 🐧🍎
```bash
git clone --branch v1.0.1 https://github.com/gGaToRr/AIM2k26_Techker.git
cd AIM2k26_Techker

# Le plus simple : compile puis lance (mode interactif, ou arguments transmis tels quels)
./lancer.sh
./lancer.sh -i "Explique le tri fusion en Java"

# Option Maven (recommandée) : compile, teste et produit un JAR exécutable
mvn package
java -jar target/aim2k26-techker-*.jar -i "Explique le tri fusion en Java"

# Option javac directe, sans Maven
javac -sourcepath ".:src" -cp ".:src/lib/*" -d bin Main.java src/menu/*.java src/cli/*.java src/nlp/*.java src/util/*.java src/gen/*.java src/llm/*.java

# Lancer la suite complète de tests TDD
./src/scripts/run_tests.sh

# Lancer en mode interactif
java -cp "bin:src/lib/*" Main

# Lancer en ligne de commande (CLI direct + inférence locale)
java -cp "bin:src/lib/*" Main -i "Crée une fonction de tri en Java" -e
```

<br />

### Option B — Windows (PowerShell / CMD 🪟)
```powershell
git clone --branch v1.0.1 https://github.com/gGaToRr/AIM2k26_Techker.git
cd AIM2k26_Techker

# Compiler le projet (séparateur ;)
javac -sourcepath ".;src" -cp ".;src/lib/*" -d bin Main.java src/menu/*.java src/cli/*.java src/nlp/*.java src/util/*.java src/gen/*.java src/llm/*.java

# Lancer en mode interactif
java -cp "bin;src/lib/*" Main

# Lancer en ligne de commande
java -cp "bin;src/lib/*" Main -i "Crée une fonction de tri en Java" -e
```

<br />

## # Extension Chrome (AIM2K26_Prompting)

L'extension améliore vos prompts directement sur ChatGPT, Claude, Gemini, Mistral, DeepSeek, Grok, Copilot, Perplexity, etc. Quand vous envoyez un prompt, elle le fait réécrire par l'outil Java et un modèle installé sur votre machine, puis le remet dans la zone de saisie : il ne reste plus qu'à l'envoyer. Rien ne quitte votre machine avant cet envoi.

**Prérequis** : Google Chrome ou Chromium, et un JDK 21+ (`javac -version`). L'outil Java est compilé automatiquement au premier usage de l'extension.

<br />

### 1. Charger l'extension
1. Clonez le dépôt (voir ci-dessus) :
   ```bash
   git clone --branch v1.0.1 https://github.com/gGaToRr/AIM2k26_Techker.git
   ```
   Gardez-le à cet emplacement : le connecteur y est rattaché. Si vous le déplacez, relancez l'installateur du connecteur (étape 2).
2. Ouvrez `chrome://extensions` et activez le **Mode développeur** (en haut à droite).
3. Cliquez sur **Charger l'extension non empaquetée** et choisissez le dossier `extension/` du projet.

L'extension **AIM2K26_Prompting** apparaît avec l'identifiant `hgikcaghkempfkljikcopdfncnhjkanl` (fixe, le connecteur en dépend).

<br />

### 2. Installer le connecteur local
Chrome interdit à une extension de lancer un programme : le connecteur fait le lien entre l'extension et l'outil Java.

```bash
# Linux / macOS
./connecteur/installer.sh
```

```powershell
# Windows : double-cliquez sur connecteur\installer.bat, ou
connecteur\installer.bat
```

Puis rechargez l'extension avec la flèche ↻ dans `chrome://extensions`.

<br />

### 3. Vérifier et installer un modèle
1. Cliquez sur l'icône de l'extension : le panneau s'ouvre (sur les pages `chrome://`, c'est une popup classique).
2. **Tester si Java est installé** : confirme que le connecteur et Java répondent.
3. **Télécharger les modèles** : choisissez un modèle (plus d'1 Go chacun). En ligne de commande : `java -cp "bin:src/lib/*" Main --models-install qwen-coder`.
4. Le moteur [llama.cpp](https://github.com/ggml-org/llama.cpp) qui exécute les modèles (~15 Mo, version figée et empreinte SHA-256 vérifiée) est installé automatiquement avec le premier modèle, dans `llama/`. S'il manque (modèles copiés à la main, par exemple), la ligne **Moteur llama.cpp** de la page des modèles l'installe, ou en ligne de commande : `java -cp "bin:src/lib/*" Main --runtime-install`.
5. La **base de prompts** (470 000 vrais prompts rangés par thème, ~120 Mo de JSON) est installée avec le premier modèle, dans `corpus/themes/`. À chaque amélioration, l'outil y cherche le prompt le plus proche du vôtre (aiguillé par les thèmes que détecte le NLP, classé selon la richesse lexicale de votre prompt) et le modèle s'en inspire. Ligne **Base de prompts** de la page des modèles, ou : `java -cp "bin:src/lib/*" Main --corpus-install`. Détails : [doc/corpus/README.md](doc/corpus/README.md).
6. **Tester mes modèles installés** : vérifie l'intégrité des fichiers et fait une génération d'essai.

Sans modèle ou sans llama.cpp, l'extension se rabat sur le moteur NLP seul et vous le signale : le résultat est utile, mais moins fiable.

<br />

### 4. Utilisation
Écrivez votre prompt sur l'un des sites pris en charge et envoyez-le (Entrée ou bouton d'envoi). L'extension l'intercepte, l'améliore (5 à 10 s) et le remet dans la zone : relisez-le, puis Entrée pour l'envoyer.

Les **Paramètres** (roue dentée du panneau) règlent l'auto-correction (amélioration et envoi sans relecture), le format de sortie, l'IA cible, la langue, le modèle, la créativité, la longueur maximale et la copie automatique.

<br />

### Désinstaller
Bouton **Supprimer** dans les Paramètres (ou **Supprimer** dans `chrome://extensions`), puis retirez le connecteur :

```bash
./connecteur/installer.sh --desinstaller          # Linux / macOS
connecteur\installer.bat -Desinstaller            # Windows
```

Les modèles restent dans `models/` : supprimez-les depuis la page Modèles de l'extension ou avec `java -cp "bin:src/lib/*" Main --models-purge`.

<br />

## # État du projet

Le projet est **fonctionnel**, **maintenu** et **100% offline** : tout tourne en mémoire sur votre machine sans aucun appel API payant ni serveur tiers.

Vous pouvez ouvrir une *issue* sur GitHub pour signaler un bug ou proposer une amélioration !

<br />

## # FAQ

<details>
<summary><b>Mes données ou prompts sont-ils envoyés vers un serveur externe ?</b></summary>

> Non, l'intégralité du traitement NLP et de la génération tourne 100% en local dans votre JVM.
</details>

<details>
<summary><b>Comment personnaliser les modèles de prompts ?</b></summary>

> Les templates Markdown sont éditables directement dans le dossier `src/genPrompt/`.
</details>

<details>
<summary><b>Erreur de Classpath sous Windows ?</b></summary>

> Utilisez le point-virgule (`;`) au lieu du deux-points (`:`) pour séparer les JARs dans `-cp`.
</details>

<details>
<summary><b>Erreur <code>package com.samskivert.mustache does not exist</code> ?</b></summary>

> Le JAR JMustache n'est pas dans le classpath. Lancez les commandes depuis la racine du projet en incluant `src/lib/*` dans `-cp`.
> Dans un IDE : VS Code → ajoutez `"java.project.referencedLibraries": ["src/lib/**/*.jar"]` dans `.vscode/settings.json` ; IntelliJ → clic droit sur `src/lib/jmustache-1.16.jar` → *Add as Library*.
</details>

<br />

## # Licence

MIT

<br />

<div align="center">

<sub>— kaets0ner —</sub>

</div>
