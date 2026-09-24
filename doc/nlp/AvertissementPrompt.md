# AvertissementPrompt

Avertit quand le prompt initial est trop court pour être bien amélioré.

- `verifier(prompt)` : renvoie `MESSAGE_PROMPT_COURT` (« Attention votre prompt initial contient trop peu d'information. ») si le prompt fait moins de `LONGUEUR_MINIMALE` (100) caractères, espaces de début et de fin exclus. Avertissement non bloquant : l'amélioration a lieu quand même.

Affiché par `Main` (sauf en `-r` et `-o json`) et transmis à l'extension dans le champ `avertissement` de `--improve-json`.
