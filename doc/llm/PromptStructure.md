# PromptStructure

Met le prompt amélioré par un modèle local au même format, quel que soit le modèle.

- `extraire(reponse)` : reconnaît les cinq sections (Rôle, Contexte, Tâche, Contraintes, Format de sortie attendu) quelle que soit leur présentation : `Role:`, `**Rôle :**`, `## Tâche`, `Section : Role`, variantes anglaises (`Task`, `Constraints`, `Output format`). Le préambule avant la première section est ignoré. Une puce n'est un titre qu'en gras : `- Format : PDF` reste une contrainte. Le rôle est ramené à sa première phrase, les puces à `- ` sans doublon.
- `structurer(reponse, langue, promptBrut)` : rend les sections en Markdown (`## Rôle` ...), dans un ordre fixe et dans la langue du prompt (fr, en, es, de). Une section absente est affichée `[à compléter]`. Les lignes recopiées de l'exemple de la consigne (`LocalLlmBackend.CONSIGNE_AMELIORATION`) sont retirées, sauf si le prompt brut en parle. Vide si moins de `SECTIONS_MINIMUM` (3) sections : l'appelant se replie sur le NLP.
- `resteSurLeSujet(prompt, motsCles)` : au moins `COUVERTURE_MINIMALE` (30 %) des mots-clés du prompt brut (4 lettres et plus, accents ignorés) doivent se retrouver dans le résultat, sinon le modèle a changé de sujet.

Utilisé par `AmeliorationCommand` (extension) et par `Main` (`-e`).
