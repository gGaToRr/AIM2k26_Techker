# Base de prompts

473 644 vrais prompts rangés par langue et par thème. À chaque amélioration, l'outil y cherche le prompt « parfait » : le vrai prompt le plus proche de celui de l'utilisateur, dont le modèle local s'inspire.

## Chaîne complète

```text
prompt de l'utilisateur
  → NLP : langue, thèmes proches, type de demande     (aiguillage)
  → base : seuls les fichiers de ces thèmes            corpus/themes/<langue>/<thème>.json
  → classement selon la richesse lexicale du prompt    RechercheThematique
  → prompt parfait (≥ 50 % des mots précis, 40 à 1 500 caractères)   PromptParfait
  → le modèle local améliore le prompt en s'inspirant de la référence
  → retour à l'utilisateur (la référence utilisée est indiquée)
```

Sans base installée, l'amélioration se fait sans référence, comme avant.

## Installation

```bash
java -cp "bin:src/lib/*" Main --corpus-install
```

Installée aussi automatiquement avec le premier modèle (page Modèles de l'extension ou `--models-install`). Une archive zip publiée dans une release GitHub, taille et SHA-256 vérifiées, extraite dans `corpus/themes/`. Aucune dépendance : téléchargement et zip sont dans le JDK.

## Format

Du JSON simple, lisible par n'importe quel outil :

- `corpus/themes/sommaire.json` : sources, thèmes de chaque langue, nombre de prompts, fichier.
- `corpus/themes/<langue>/<thème>.json` : tableau JSON, un prompt par ligne, les mieux notés d'abord :
  `id`, `source`, `langue`, `categorie` (type de demande), `themes`, `score` (qualité NLP), `qualite` (note humaine oasst2), `auteur` (identifiant anonyme), `texte`.

Un prompt proche de plusieurs thèmes (3 au plus) figure dans chacun d'eux ; `divers` regroupe ceux sans thème net.

## Recherche

1. **Thèmes** : `nlp.ThemeClassifier` (graines + vocabulaire appris + technologies détectées) donne les thèmes proches. Seuls leurs fichiers sont lus.
2. **Mots** : BM25 (chaque mot pèse selon sa rareté, un prompt long compte un peu moins) multiplié par la couverture (part des mots précis de l'utilisateur présents, y compris ceux que la base ne connaît pas).
3. **Richesse lexicale** : le poids des mots dans le classement dépend du nombre de mots précis du prompt, de 50 % (prompt pauvre : le thème, le type de demande et la qualité comptent davantage) à 85 % (prompt riche).

## Sources et licences

| Corpus | Contenu | Licence |
|---|---|---|
| [OpenAssistant oasst2](https://huggingface.co/datasets/OpenAssistant/oasst2) | Prompts de départ écrits par des bénévoles, note de qualité humaine | Apache 2.0 |
| [WildChat-1M](https://huggingface.co/datasets/allenai/WildChat-1M) (AllenAI) | Premier message de vraies conversations avec ChatGPT | ODC-BY |

Filtres appliqués à la construction : premier message seulement, fr / en / es / de, 5 à 8 000 caractères, oasst2 sans spam, données personnelles ni contenu inapproprié, doublons exacts retirés.

## Outils (Java, sans dépendance)

Ils lisent la base elle-même : elle se suffit à elle-même.

```bash
java -Xmx12g -cp "bin:src/lib/*" corpus.AnalyseCorpus               # NLP sur toute la base : corpus/rapport_nlp.md
java -Xmx14g -cp "bin:src/lib/*" corpus.ApprendreThemes             # vocabulaire des thèmes : src/genPrompt/themes/modele.tsv
java -Xmx14g -cp "bin:src/lib/*" corpus.ConstruireIndex             # reclasse la base après un nouvel apprentissage
java -cp "bin:src/lib/*" corpus.RechercheThematique "mon prompt" 5  # prompts les plus proches
```

## Apprentissage des thèmes

`src/genPrompt/themes/graines.properties` donne quelques mots-clés sûrs par thème, communs ou propres à une langue. `ApprendreThemes` en tire le vocabulaire de chaque thème : exemples désignés par les graines, poids de chaque mot comptés en **auteurs distincts** (quelques utilisateurs envoient des centaines de variantes d'un même prompt), puis une seconde passe de reclassement. Le rapport `corpus/themes_apprentissage.md` montre les mots appris par thème.
