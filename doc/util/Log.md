# 🪵 Documentation : `src/util/Log.java`

## 📌 Rôle du Fichier

`Log` est le journal de diagnostic de l'application. Il repose sur `java.util.logging` — aucune dépendance externe n'est ajoutée — et expose une façade statique minimale.

---

## 🧭 Deux sorties à ne pas confondre

C'est la distinction qui justifie l'existence de ce fichier.

| Flux | Contenu | Destination |
|:---|:---|:---|
| **Produit** | Prompt généré, onboarding, barre de progression, rapport `-V` | `stdout` (ou le `PrintStream` injecté) |
| **Diagnostic** | Incidents, exceptions non propagées, avertissements | `stderr`, via `Log` |

Les envoyer au même endroit casserait les usages en pipe :

```bash
java -cp "bin:src/lib/*" Main -r -i "Trie ce tableau" | pbcopy
```

En mode `-r/--raw`, `stdout` ne doit contenir que le prompt. Un avertissement « fichier introuvable » mélangé au flux finirait collé dans le presse-papiers.

> C'est pourquoi les classes qui produisent de l'interface (`LlmEngine`, `ModelInstaller`) gardent leur `PrintStream` injecté : leur sortie **est** le produit, ce n'est pas du log.

---

## 🔊 Niveaux

| Niveau | Usage |
|:---|:---|
| `SILENCIEUX` | N'émet rien |
| `ERREUR` | Échec qui empêche l'opération demandée |
| `AVERTISSEMENT` | **Défaut** — anomalie que l'utilisateur doit connaître |
| `INFO` | Étapes notables du déroulement |
| `DEBUG` | Détail interne, dont les exceptions non propagées |

Le seuil suit le flag CLI, branché dans `Main` :

```java
Log.configurerDepuisVerbose(cliArgs.isVerbose()); // -V -> DEBUG, sinon AVERTISSEMENT
```

---

## 🕵️ `exceptionIgnoree` — le cas des `catch` silencieux

Le projet comportait dix `catch (Exception ignored) {}`. Chacun est un choix légitime — le flux nominal doit continuer — mais sans trace, un vrai problème (permissions, disque plein, binaire manquant) devenait invisible et indébogable.

```java
} catch (IOException e) {
    Log.exceptionIgnoree("Lecture du template " + chemin, e);
}
```

La sortie, en mode verbeux uniquement :

```
[DEBUG] Lecture du template /…/learning/guide_debutant.md -> IOException : Permission denied
```

Le comportement du programme est inchangé : l'exception n'est toujours pas propagée. Seule son invisibilité disparaît.

---

## 🧪 Tests

`Log.brancherSortie(PrintStream)` redirige le journal, ce qui permet de capturer et d'asserter sur les messages sans toucher à `System.err`.
