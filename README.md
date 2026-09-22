# Prompting_Tool (AIM2k26_Techker)
Vous en avez marre des réponses vagues ou incomplètes des LLMs ? Le moteur **[Prompting Tool]** est un optimiseur de requêtes open-source : il analyse votre consigne brute via un pipeline NLP et génère un Super-Prompt ultra-structuré (Meta-Prompting).

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:09090b,50:7c3aed,100:c084fc&height=200&section=header&text=AIM2k26_Techker&fontSize=65&fontColor=ffffff&animation=fadeIn&desc=Moteur%20NLP%20et%20Meta-Prompting%20pour%20LLM&descSize=18&descAlignY=70&descAlign=50" />
<br />

![Status](https://img.shields.io/badge/status-Working-green?style=flat-square)
![Type](https://img.shields.io/badge/type-open%20source-purple?style=flat-square)
![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
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

**Prérequis** : Java 17+ (`java -version`). La bibliothèque JMustache est incluse dans `src/lib/`.

<br />

### Option A — Linux / macOS 🐧🍎
```bash
git clone https://github.com/gGaToRr/AIM2k26_Techker.git
cd AIM2k26_Techker

# Compiler le projet
javac -sourcepath ".:src" -cp ".:src/lib/*" -d bin Main.java src/menu/*.java src/cli/*.java src/nlp/*.java src/gen/*.java src/llm/*.java

# Lancer la suite complète de 129 tests TDD
./src/scripts/run_tests.sh

# Lancer en mode interactif
java -cp "bin:src/lib/*" Main

# Lancer en ligne de commande (CLI direct + inférence locale)
java -cp "bin:src/lib/*" Main -i "Crée une fonction de tri en Java" -e
```

<br />

### Option B — Windows (PowerShell / CMD 🪟)
```powershell
git clone https://github.com/gGaToRr/AIM2k26_Techker.git
cd AIM2k26_Techker

# Compiler le projet (séparateur ;)
javac -sourcepath ".;src" -cp ".;src/lib/*" -d bin Main.java src/menu/*.java src/cli/*.java src/nlp/*.java src/gen/*.java src/llm/*.java

# Lancer en mode interactif
java -cp "bin;src/lib/*" Main

# Lancer en ligne de commande
java -cp "bin;src/lib/*" Main -i "Crée une fonction de tri en Java" -e
```

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
