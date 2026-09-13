# KAETS-Prompt-Engine (Project_Ia)
Comme vous le savez, formuler un prompt efficace pour les LLMs (ChatGPT, Claude, Gemini) est un défi majeur : requêtes imprécises, manque de contexte, hallucinations et formats inadaptés réduisent considérablement la qualité des réponses. Le moteur **[KAETS Prompt IA]** est désormais accessible et open-source. Il analyse, nettoie, évalue et convertit automatiquement n'importe quelle consigne brute en un **Super-Prompt d'élite optimisé (Meta-Prompting)**.

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:09090b,50:7c3aed,100:c084fc&height=220&section=header&text=KAETS%20Prompt%20IA&fontSize=65&fontColor=ffffff&animation=fadeIn&desc=Moteur%20NLP%20%26%20Optimisateur%20de%20Prompts%20pour%20LLM&descSize=18&descAlignY=70&descAlign=50" />
<br />

![Status](https://img.shields.io/badge/status-Working-green?style=flat-square)
![Type](https://img.shields.io/badge/type-open%20source-purple?style=flat-square)
![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)
![Offline](https://img.shields.io/badge/100%25-Offline%20%26%20Local-blue?style=flat-square)
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
2. Identification des cas limites (entrées invalides, concurrence, erreurs réseau).
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
|  🛡️  | **Safety Advisor** | Détecte préventivement les termes sensibles (armes, cyberattaques, jailbreak) susceptibles de bloquer les filtres des LLMs (OpenAI, Anthropic, Gemini) avec avertissement pédagogique sans bloquer l'exécution. |
|  🧹  | **Sanitization & Normalisation** | Nettoyage de ponctuation, passage en minuscules, suppression des diacritiques (accents), découpage CamelCase/snake_case et filtrage des Stop Words (FR & EN). |
|  🔤  | **Lemmatisation & NLP** | Dictionnaire étendu de racines métier (code, dev, debug, traduction, rédaction, questions), racinisation des suffixes et calcul des densités syntaxiques. |
|  🔬  | **Tech Stack & Contexte** | Reconnaissance fine des langages (Java, Python, C++, C#, JS/TS, Rust, Go, SQL...), frameworks web (React, Vue, Tailwind...), moteurs de jeux (Unity, Unreal, Godot) et contextes académiques (Epitech, 42). |
|  📊  | **Scoring Qualité (0-100)** | Évaluation du prompt sur 3 piliers (Clarté de consigne, Contexte fourni, Contraintes & format attendu) avec points forts et pistes d'amélioration. |
|  🪙  | **Token Counter & Coûts** | Estimation BPE du nombre de sous-mots (tokens), calcul du coût estimé ($/1M tokens) et empreinte dans la fenêtre de contexte (128K). |
|  🎯  | **Classification d'Intention** | Modèle de scoring probabiliste multi-classes (Softmax normalisé) avec niveau de confiance (HIGH, MEDIUM, LOW) et justification sémantique. |
|  🚀  | **Meta-Prompt Engine** | Retrait du bruit conversationnel ("salut", "merci d'avance"), injection d'un persona d'expert, directives strictes (SOLID, CoT, zéro placeholder) et rendu de templates Mustache ultra-rapide. |

<br />

## # Matrice des Intentions & Templates

Le moteur adapte dynamiquement la structure du prompt de sortie selon l'intention et le niveau de détail :

| Catégorie (`TypeOfPrompt`) | Sous-Type Détecté | Cas d'Usage & Spécialisation |
| :------------------------- | :---------------- | :--------------------------- |
| **`CODE`** | `generation` | Écriture de code neuf complet, production-ready et testable |
| | `frontend_ui` | Design UI/UX, CSS, Tailwind, layouts réactifs et composants web |
| | `architecture` | Conception modulaire, arborescence complète en ASCII Tree |
| | `debug` | Analyse d'erreurs, stack traces, identification de root cause |
| | `refactor` | Nettoyage, optimisation de performance et respect des principes SOLID |
| | `review` | Audit de sécurité, vérification de conformité et bonnes pratiques |
| **`TRANSLATE`** | `technical` | Traduction technique fidèle avec détection de la langue cible |
| **`CORRECTANSWERS`** | `proofreading` | Correction orthographique, grammaticale et typographique |
| | `rewrite` | Réécriture stylistique, reformulation et amélioration du ton |
| **`CREATION`** | `storytelling` | Narration créative, rédaction d'histoires, scénarios et dialogues |
| | `brainstorming` | Génération d'idées originales, concepts et perspectives divergentes |
| **`FACTUALQUESTIONS`** | `feynman` | Vulgarisation par la technique de Feynman (explications simples) |
| | `concept_guide` | Traitement exhaustif de notions isolées ou synthèses thématiques |
| | `deep_technical` | Réponses théoriques et scientifiques poussées |

<br />

## # Architecture & Pipeline NLP

```
┌───────────────────────────────────────────────────────────┐
│                    PROMPT UTILISATEUR                     │
│               (Texte brut / conversationnel)              │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│              1. SÉCURITÉ & SANITIZATION                   │
│  • SafetyAdvisor : Détection préventive de termes à risque│
│  • Sanitzer      : Nettoyage ponctuation & normalisation  │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│           2. TOKENISATION & LEMMATISATION                 │
│  • Lemmatizer   : Accents, CamelCase, StopWords           │
│  • Dictionnaire de racines & Racinisation par suffixes    │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│                3. ANALYSES MULTI-AXES                     │
│  • Densité de code & Syntaxe      • Détection FR / EN     │
│  • Mode Commande vs Question      • TechStack & Contexte  │
│  • Compteur BPE & Coût ($)        • Diagnostic Qualité /100│
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│              4. CLASSIFICATION & SCORING                  │
│  • Distribution probabiliste (CODE, TRANSLATE, etc.)      │
│  • Indice de confiance (HIGH / MEDIUM / LOW)              │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│           5. META-PROMPT ENGINE & RENDU MUSTACHE          │
│  • Filtrage du bruit ("salut", "merci", "stp")            │
│  • Injection Persona Expert, Contexte technique & Règles  │
│  • Compilation Markdown avec JMustache (genPrompt/)       │
└─────────────────────────────┬─────────────────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────┐
│              SUPER-PROMPT OPTIMISÉ POUR LLM               │
└───────────────────────────────────────────────────────────┘
```

<br />

## # Installation & Utilisation

### Prérequis

- **Java JDK 17** ou version ultérieure installé sur votre machine.
  > Vérifiez avec : `java -version` et `javac -version`
- Le projet est entièrement autonome : la bibliothèque de templating **JMustache** est déjà incluse dans le dossier `lib/jmustache-1.16.jar`.

<br />

### Option A — Clonage & Lancement rapide (Linux / macOS / Windows)

```bash
# 1. Cloner le dépôt
git clone https://github.com/gGaToRr/Project_Ia.git

# 2. Se rendre dans le dossier
cd Project_Ia
```

#### Sur Linux / macOS 🐧🍎
```bash
# Compiler le projet
javac -cp ".:lib/jmustache-1.16.jar" -d bin Main.java menu/Menu.java nlp/*.java gen/*.java

# Lancer l'application
java -cp "bin:lib/jmustache-1.16.jar" Main
```

#### Sur Windows (PowerShell / CMD) 🪟
```powershell
# Compiler le projet (séparateur point-virgule sur Windows)
javac -cp ".;lib/jmustache-1.16.jar" -d bin Main.java menu/Menu.java nlp/*.java gen/*.java

# Lancer l'application
java -cp "bin;lib/jmustache-1.16.jar" Main
```

<br />

### Option B — Téléchargement de l'archive (.zip)

1. Téléchargez l'archive `.zip` du projet depuis GitHub.
2. Décompressez l'archive dans le dossier de votre choix.
3. Ouvrez un terminal dans le dossier `Project_Ia` décompressé.
4. Exécutez les commandes de compilation et de lancement indiquées ci-dessus.

<br />

## # Personnalisation des Templates

Tous les modèles de prompts Markdown se trouvent dans le sous-dossier `genPrompt/` :
- `genPrompt/code/` : `generation.md`, `architecture.md`, `debug.md`, `frontend_ui.md`, `refactor.md`, `review.md`
- `genPrompt/correction/` : `proofreading.md`
- `genPrompt/creation/` : `storytelling.md`
- `genPrompt/questions/` : `concept_guide.md`, `feynman.md`
- `genPrompt/translate/` : `technical.md`

Vous pouvez éditer ces fichiers ou ajouter vos propres tags Mustache (`{{techStackSummary}}`, `{{language}}`, `{{cleanedMission}}`, `{{#autoConstraints}}...{{/autoConstraints}}`) pour adapter le style des réponses à vos besoins.

<br />

## # État du projet

Le projet est **fonctionnel**, **ultra-léger** et **maintenu**. 
L'ensemble des étapes d'analyse, de lemmatisation et de rendu s'exécute **100% en local** en quelques millisecondes, **sans nécessiter d'appel API payant, ni connexion internet, ni conteneur lourd**.

Vous pouvez ouvrir une *issue* ou une *pull request* sur GitHub pour signaler un bug ou proposer une amélioration !

<br />

## # FAQ

<details>
<summary><b>Mon prompt contient un terme technique mais n'est pas classé en Code, pourquoi ?</b></summary>

> Le classifier évalue l'équilibre global des lemmes et la présence de verbes d'action. Si vous tapez simplement un mot isolé sans contexte (ex: `Python`), le moteur bascule intelligemment sur `FACTUALQUESTIONS` (`concept_guide`) pour fournir une synthèse encyclopédique plutôt que de générer du code dans le vide.
</details>

<details>
<summary><b>Comment fonctionne l'estimation de coût et de tokens ?</b></summary>

> La classe `TokenCounter` reproduit le découpage en sous-mots selon la tokenisation BPE (Byte Pair Encoding) utilisée par les modèles récents (GPT-4, Claude, Gemini). Le coût est calculé sur une base indicative standard de ~2.50$ pour 1 million de tokens.
</details>

<details>
<summary><b>Erreur "Could not find or load main class Main" sous Windows ?</b></summary>

> Sous Windows, le séparateur de classpath doit être un point-virgule (`;`) et non un deux-points (`:`). Utilisez la commande : `java -cp "bin;lib/jmustache-1.16.jar" Main`.
</details>

<details>
<summary><b>Mes données ou prompts sont-ils envoyés vers un serveur externe ?</b></summary>

> **Non, aucun.** Tous les algorithmes (nettoyage, scoring, lemmatisation, templating) tournent intégralement en mémoire sur votre JVM locale. Vos prompts restent strictement confidentiels.
</details>

<br />

## # Licence

Ce projet est distribué sous licence **MIT**. Vous êtes libre de l'utiliser, l'adapter et l'intégrer dans vos propres projets.

<br />

<div align="center">

<sub>— kaets0ner —</sub>

</div>
