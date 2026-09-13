# 🧠 Documentation : `src/llm/LocalLlmBackend.java`

## 🎯 Rôle du Fichier

[`LocalLlmBackend.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/LocalLlmBackend.java) implémente `LlmBackend` et exécute réellement l'inférence, en choisissant entre deux modes selon la disponibilité d'un runtime natif.

---

## 🔍 Détection du Runtime (`detectRunnerBinary`)

L'ordre de recherche est le suivant :

1. **Runtime installé par l'application** : recherche récursive (profondeur 4) d'un binaire `llama-cli` (ou `llama-cli.exe` sous Windows) dans le répertoire `config.getRepertoireRuntime()`, via [`RuntimeInstaller.getRuntimeBinaryPath`](RuntimeInstaller.md). C'est le cas normal après le premier lancement.
2. **Chemins candidats fixes** : `llama-cli`, `bin/llama-cli`, `./llama-cli`, `/usr/local/bin/llama-cli`, `/usr/bin/llama-cli`.
3. **PATH système** : `which llama-cli`.

Si aucune de ces trois étapes n'aboutit, `generate()` retombe sur `runEmbeddedInference` (mode simulé/dégradé).

---

## ⚠️ Mode Simulé (`runEmbeddedInference`)

Ce mode renvoie une réponse **structurée mais codée en dur** par `ModelType`, utile pour ne jamais bloquer l'utilisateur (environnement de test, plateforme non supportée, échec réseau). Il ne doit **jamais** être confondu avec une vraie inférence : `LlmEngine` affiche désormais un avertissement explicite quand ce mode est utilisé faute de runtime installable.

---

## 🚀 Mode Natif (`runNativeInference`)

Lance `llama-cli -m <modele.gguf> -p <prompt> -n <maxTokens> --temp <temperature> --no-display-prompt` via `ProcessBuilder`, et streame la sortie token par token (découpage sur espaces/ponctuation) au fur et à mesure de la lecture du flux.

**Important** : le binaire téléchargé par `RuntimeInstaller` dépend de bibliothèques partagées situées dans le même dossier que lui (`RUNPATH=$ORIGIN`). Il ne faut donc jamais déplacer `llama-cli` hors de son dossier d'installation — c'est pourquoi `getRuntimeBinaryPath` référence toujours le binaire à l'intérieur de son arborescence extraite plutôt que de le copier ailleurs.
