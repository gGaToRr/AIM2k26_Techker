# 📄 Documentation : `scripts/run_tests.sh`

## 📌 Rôle du Script
`scripts/run_tests.sh` est le script Shell Bash automatisant la compilation du projet et l'exécution de l'intégralité de la suite de tests TDD.

---

## ⚙️ Déroulement du Script

1. **Vérification du Répertoire** :
   - Se positionne à la racine du projet (répertoire parent de `scripts/`).

2. **Compilation du Code Source et des Tests** :
   - Crée le dossier `bin/` si nécessaire.
   - Compile l'ensemble des fichiers `.java` (`src/nlp/`, `src/gen/`, `src/menu/`, `src/cli/`, `test/`, `Main.java`) avec le classpath `lib/jmustache-1.16.jar:.`.
   - Si la compilation échoue, le script affiche un message d'erreur et s'arrête avec le code de sortie `1`.

3. **Exécution de la Suite Globale** :
   - Lance la classe `test.AllTestSuite` avec le classpath `bin:lib/jmustache-1.16.jar:.`.
   - Transmet le code de retour à l'environnement d'appel (idéal pour les workflows CI/CD GitHub Actions).

---

## 👨‍💻 Utilisation en Ligne de Commande

```bash
# Exécution directe
./scripts/run_tests.sh
```
