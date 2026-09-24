# 📄 Documentation : `src/cli/CliParser.java`

## 📌 Rôle du Fichier
`src/cli/CliParser.java` est le **moteur d'analyse syntaxique (parser)** de la ligne de commande. Il prend en entrée le tableau brut `String[] args` et produit un objet `CliArgs` validé et normalisé.

---

## ⚙️ Capacités du Parser

1. **Prise en charge des drapeaux courts et longs** :
   - `-h` / `--help` : Manuel d'utilisation.
   - `-v` / `--version` : Version du projet.
   - `-V` / `--verbose` : Rapport d'analyse NLP.
   - `-i` / `--instruction` : Instruction directe.
   - `-c` / `--code` : Code inline ou fichier source.
   - `-a` / `--agent` : Modèle/agent IA cible.
   - `-o` / `--output` : Format (json/md/txt) ou fichier de sortie.
   - `-t` / `--template` : Forçage de template.
   - `-d` / `--domain` : Forçage de domaine métier.
   - `-l` / `--language` : Langue cible.
   - `-ml` / `--models-list`, `-mi` / `--models-info`, `-md` / `--models-delete`, `-mp` / `--models-purge` : gestion du cycle de vie des modèles locaux (voir [`ModelInstaller`](../llm/ModelInstaller.md)). Ces commandes court-circuitent le pipeline de génération et rendent directement un code de sortie.
   - `-r` / `--raw` : Sortie brute sans bannière.
   - `-f` / `--file` : Fichier contenant le prompt.
   - `-n` / `--dry-run` : Analyse seule.
   - `-C` / `--clipboard` : Copie presse-papiers.

2. **Support de multiples syntaxes** :
   - Espace séparateur : `-i "Mon prompt"`
   - Signe égal long : `--instruction="Mon prompt"`
   - Signe égal court : `-i="Mon prompt"`
   - Arguments positionnels libres : `java Main Explique le tri fusion` -> converti en instruction `"Explique le tri fusion"`.

3. **Résolution automatique des fichiers** :
   - Pour `-c chemin/vers/Fichier.java` ou `-f prompt.txt`, le parser vérifie l'existence du fichier et lit automatiquement son contenu en UTF-8.

---

## 🛠️ Méthodes Principales

* `parse(String[] args)` : Analyse le tableau de chaînes et construit le `CliArgs`.
* `getHelpBanner()` : Génère le manuel d'utilisation formaté avec la bannière d'accueil encadrée du projet.
* `getVersionInfo()` : Retourne les métadonnées de version, auteur et environnement JVM.
* `formatVerboseReport(PromptProfile profile)` : Génère un rapport d'analyse sémantique complet et structuré en 8 sections.

---

## 👨‍💻 Exemple d'Utilisation

```java
String[] cmdArgs = {"-i", "Corrige ce bug", "-c", "src/Main.java", "-V"};
CliArgs args = CliParser.parse(cmdArgs);

if (args.isHelp()) {
    System.out.println(CliParser.getHelpBanner());
}
```

---

## 🗂️ Table de Dispatch

`parse` ne contient plus de chaîne de `if/else if`. Chaque option est déclarée une fois dans `construireTableOptions()`, et la boucle de parsing est générique :

```java
declarerDrapeau(table, "verbose", "-V", "--verbose", (b, v) -> b.verbose(true));
declarerValeur(table, "agent",   "-a", "--agent",   CliArgs.Builder::agent);
```

| Déclaration | Effet |
|:---|:---|
| `declarerDrapeau` | Option sans valeur : l'alias suffit à l'activer |
| `declarerValeur` | Option consommant l'argument suivant, **ou** la partie après `=` |

La forme `--option=valeur` est dérivée automatiquement : elle n'a pas à être déclarée. Le découpage sur `=` n'a lieu que si l'argument commence par `-`, afin qu'un argument positionnel comme `x=y+2` reste intact, et seul le **premier** `=` sépare le flag de sa valeur (`--instruction=calcule a=b` passe la valeur entière).

**Ajouter un flag** se résume désormais à une ligne dans la table — au lieu des trois branches court / long / `=` qu'il fallait écrire auparavant pour chacun des 8 flags à valeur.

### Règles conservées

| Situation | Comportement |
|:---|:---|
| Flag inconnu (`--inconnu`) | Ignoré en silence, et **non** traité comme argument positionnel |
| Flag à valeur en fin de ligne (`-t`) | Option ignorée, aucune exception |
| Flag répété (`-a gpt -a claude`) | La dernière valeur l'emporte |
| Arguments positionnels | Joints par des espaces et utilisés comme instruction si `-i` est absent |
| `-f` / `--file` | Alimente à la fois `filePath` et `instruction` (contenu lu) |
