# ⚙️ Documentation : Build et Intégration Continue

## 📦 `pom.xml`

La dépendance JMustache n'avait de version déclarée **nulle part** : elle ne vivait que dans le nom du JAR commité (`src/lib/jmustache-1.16.jar`). Elle est désormais une dépendance Maven explicite.

```bash
mvn test                     # compile + suite TDD complète
mvn package                  # + JAR exécutable dans target/
mvn package -DskipTests      # sans la suite
java -jar target/aim2k26-techker-*.jar -i "Explique le tri fusion"
```

### Disposition non standard, assumée

Les sources vivent dans `src/<module>/` et `Main.java` est à la racine — ce n'est pas la convention Maven (`src/main/java`). Plutôt que de déplacer 55 fichiers et d'invalider toutes les commandes documentées, le `pom.xml` **décrit la disposition réelle** via `sourceDirectory` et des `includes` explicites.

### Le framework de test n'est pas JUnit

Surefire ne sait pas découvrir un `TestRunner` maison. Il est donc neutralisé (`skipTests`), et la suite est lancée par `exec-maven-plugin` sur la phase `test`.

`AllTestSuite` sort avec un code non nul en cas d'échec, **ce qui fait échouer le build**. Vérifié en cassant volontairement une assertion : `mvn test` rend alors le code 1.

`exec-maven-plugin` ignore `-DskipTests` par défaut ; la configuration le rebranche explicitement.

### JAR exécutable

`maven-dependency-plugin` copie les dépendances dans `target/lib/`, que le manifeste référence via `classpathPrefix`. Le JAR produit fonctionne donc tel quel, rendu Mustache compris.

---

## 🔁 `.github/workflows/ci.yml`

Le dépôt contenait un workflow **jamais activé** : `src/ci/github-actions-test.yml`, avec en commentaire « À placer dans `.github/workflows/test.yml` ». Il n'y a jamais été placé — donc rien ne vérifiait qu'une PR compilait. Ce fichier est remplacé par un workflow réellement actif.

Déclenchement : push et pull request sur `main`, `premain`, `dev`.

| Job | Rôle |
|:---|:---|
| `build` | `mvn test`, puis `mvn package`, vérifie que le JAR démarre, publie l'artefact |
| `compilation-sans-maven` | Exécute `src/scripts/run_tests.sh` |

### Pourquoi deux jobs

Le README documente une compilation `javac` directe, sans Maven. Si ce chemin casse — un nouveau package oublié dans la liste, par exemple — l'instruction d'installation devient fausse pour quiconque n'a pas Maven. Le second job garde cette promesse vérifiée.

> C'est exactement ce qui s'est produit lors de l'ajout de `src/util/` : la commande du README a dû être mise à jour pour inclure le nouveau package.
