# 📄 Documentation : `Main.java`

## 📌 Rôle du Fichier
`Main.java` est le **point d'entrée principal (entrypoint)** de l'application. Son rôle est d'orchestrer l'ensemble du cycle de vie de l'exécution, depuis la lecture des arguments jusqu'à la restitution du prompt optimisé.

---

## ⚙️ Fonctionnement Étape par Étape

1. **Parsing des Arguments CLI (`cli.CliParser.parse(args)`)** :
   - Si l'utilisateur demande l'aide (`-h`), affiche le manuel et s'arrête (`exit`).
   - Si l'utilisateur demande la version (`-v`), affiche les informations de build et s'arrête.
   - Récupère l'instruction passée via `-i`, `-c`, `-f` ou bascule sur le mode interactif `menu.demanderPrompt()`.

2. **Validation de l'Entrée** :
   - Vérifie que le prompt n'est pas vide ou composé uniquement d'espaces.

3. **Analyse de Sécurité Préventive (`nlp.SafetyAdvisor.analyser`)** :
   - Vérifie la présence de termes sensibles (mots de passe, clés d'API, informations confidentielles) et affiche un avertissement préventif sans bloquer l'exécution.

4. **Analyse NLP Complète (`nlp.Lemmatizer.analyser`)** :
   - Déclenche le pipeline d'analyse pour produire une fiche sémantique `nlp.PromptProfile`.
   - Si le flag `-V` (verbose) est activé, affiche le rapport détaillé d'analyse.
   - Si le flag `-n` (dry-run) est activé, arrête le programme après l'affichage des métriques sans compiler de prompt.

5. **Génération du Prompt Optimisé (`gen.MetaPromptEngine.genererPromptOptimise`)** :
   - Injecte le profil et les options (forçage de template, domaine, langue, agent IA) dans le moteur Mustache.

6. **Formatage & Export** :
   - Si `-o json` est demandé, convertit le résultat en JSON structuré.
   - Si `-o <chemin>` est spécifié, écrit le résultat dans le fichier cible.
   - Si `-C` est activé, copie le résultat dans le presse-papiers système.
   - Affiche le résultat sur la console standard (ou format épuré si `-r`).

---

## 👨‍💻 Exemple d'Exécution Simple

```bash
# Exécution interactive standard
java -cp "bin:lib/jmustache-1.16.jar" Main

# Exécution en ligne de commande avec verbose
java -cp "bin:lib/jmustache-1.16.jar" Main -i "Explique le tri fusion en Java" -V
```
