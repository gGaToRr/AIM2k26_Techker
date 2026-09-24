# FormatSortie

Branche l'option `-o/--output` sur le rendu du prompt optimisé.

- `depuis(CliArgs)` : `txt`/`text`/`texte`, `md`/`markdown`, `json` (insensible à la casse). Toute autre valeur est un chemin de fichier dont l'extension décide du format : `.json` → JSON, `.txt` → TXT, sinon MD. Sans `-o` : MD.
- `fichierCible(CliArgs)` : le chemin d'export quand `-o` n'est pas un mot-clé de format.
- `rendre(profil, prompt, options)` : MD renvoie le prompt tel quel, TXT le passe par `versTexteBrut`, JSON délègue à `MetaPromptEngine.genererExportJson`.
- `versTexteBrut(markdown)` : retire les symboles Markdown (titres, gras, italique, code en ligne, clôtures de blocs) en gardant le texte. Les balises XML des templates sont conservées.
