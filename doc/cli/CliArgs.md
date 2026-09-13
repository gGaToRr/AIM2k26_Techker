# 📄 Documentation : `src/cli/CliArgs.java`

## 📌 Rôle du Fichier
`src/cli/CliArgs.java` est un **Record Java immuable** servant de conteneur typé pour l'ensemble des options, drapeaux et arguments fournis lors de l'appel en ligne de commande.

---

## 📋 Structure des Données

```java
public record CliArgs(
        boolean isHelp,          // -h, --help
        boolean isVersion,       // -v, --version
        boolean isVerbose,       // -V, --verbose
        boolean isRaw,           // -r, --raw
        boolean isDryRun,        // -n, --dry-run
        boolean isClipboard,     // -C, --clipboard
        String instruction,      // -i, --instruction
        String code,             // -c, --code
        String agent,            // -a, --agent
        String output,           // -o, --output
        String template,         // -t, --template
        String domain,           // -d, --domain
        String language,         // -l, --language
        String filePath          // -f, --file
)
```

---

## 🛠️ Méthodes Clés & Helpers

* `hasInstruction()` : Retourne `true` si une instruction textuelle est renseignée.
* `hasCode()` : Retourne `true` si un extrait de code ou fichier source est injecté.
* `hasAgent()`, `hasOutput()`, `hasTemplate()`, `hasDomain()`, `hasLanguage()`, `hasFilePath()` : Vérificateurs de présence non vide.
* `getFullPrompt()` : Combine harmonieusement l'instruction et le code injecté dans un bloc Markdown formaté (` ``` `) pour l'analyse NLP.
* `CliArgs.builder()` : Pattern Builder fluide pour instancier facilement un objet `CliArgs`.

---

## 👨‍💻 Exemple d'Instanciation avec le Builder

```java
CliArgs args = CliArgs.builder()
        .instruction("Explique les microservices")
        .agent("claude")
        .verbose(true)
        .build();

System.out.println("Prompt : " + args.getFullPrompt());
```
