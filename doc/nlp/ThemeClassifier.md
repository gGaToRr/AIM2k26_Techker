# ThemeClassifier

Thème d'un prompt (programmation, cuisine, voyage… 27 thèmes), à partir d'un vocabulaire appris sur 470 000 vrais prompts (voir `doc/corpus/README.md`).

- Fichiers : `src/genPrompt/themes/graines.properties` (mots-clés écrits à la main, par langue) et `src/genPrompt/themes/modele.tsv` (vocabulaire appris, généré par `corpus.ApprendreThemes`).
- `scorer(texte, langue)` : somme des poids des mots du prompt pour chaque thème, plus `POIDS_GRAINE` (3) par graine trouvée. Une technologie reconnue par `TechStackDetector` vaut une graine de Programmation (ou de Web pour HTML/CSS).
- `themesProches(texte, langue)` : les thèmes d'au moins 60 % du meilleur score (3 au plus), ou `divers` si le meilleur est sous `SCORE_MIN` (une graine).
- Les thèmes sont dans `PromptProfile.themes()`, affichés par le CLI et transmis au modèle local dans la demande d'amélioration.
