# ⬇️ Documentation : `src/llm/RuntimeInstaller.java`

## 🎯 Rôle du Fichier

[`RuntimeInstaller.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/RuntimeInstaller.java) télécharge et installe automatiquement le **runtime natif d'inférence** (`llama-cli`, issu de [ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp)) adapté à la machine de l'utilisateur, pour que l'exécution locale soit réelle et non simulée (voir [`LocalLlmBackend`](LocalLlmBackend.md)).

Sans ce module, l'application ne fournissait que les **poids** du modèle ([`ModelInstaller`](ModelInstaller.md)) : sans le binaire `llama-cli` déjà présent manuellement sur le système, l'inférence retombait systématiquement sur une réponse simulée.

---

## 🧭 Détection de Plateforme

`detecterPlateforme()` combine `os.name` et `os.arch` en une clé `<os>-<arch>` : `linux-x64`, `linux-arm64`, `macos-x64`, `macos-arm64`, `windows-x64`, `windows-arm64` (ou `inconnu`).

---

## 🎯 Sélection de l'Asset (`choisirAsset`)

Chaque release `llama.cpp` publie de nombreuses variantes par plateforme : builds CPU, mais aussi CUDA, Vulkan, SYCL, ROCm, OpenVINO, OpenCL... Ces variantes accélérées nécessitent des drivers/matériel spécifiques non garantis présents.

**Choix retenu : toujours le build CPU simple**, robuste sur n'importe quelle machine :
- `llama-b<tag>-bin-ubuntu-x64.tar.gz` / `ubuntu-arm64`
- `llama-b<tag>-bin-macos-x64.tar.gz` / `macos-arm64` (Apple Silicon)
- `llama-b<tag>-bin-win-cpu-x64.zip` / `win-cpu-arm64`

Les variantes contenant `cuda`, `vulkan`, `sycl`, `rocm`, `openvino` ou `opencl` sont explicitement exclues.

---

## 📦 Extraction (deux formats selon l'OS)

| Plateforme | Format d'archive | Méthode |
| :--- | :--- | :--- |
| Linux / macOS | `.tar.gz` | `extraireTarGz` — délègue à la commande système `tar` (aucune dépendance Java pour le tar) |
| Windows | `.zip` | `extraireZip` — `java.util.zip.ZipInputStream`, avec **protection anti path-traversal (zip-slip)** : toute entrée résolvant en dehors du dossier cible est rejetée |

⚠️ Le binaire `llama-cli` dépend de bibliothèques partagées (`.so`) situées **à côté de lui** dans l'archive (`RUNPATH=$ORIGIN`). L'extraction préserve donc toute l'arborescence de l'archive ; `getRuntimeBinaryPath` fait ensuite une **recherche récursive** (profondeur 4) du binaire dans cette arborescence plutôt que de supposer un chemin fixe.

---

## 🌐 Résolution de la Dernière Version

L'API `https://api.github.com/repos/ggml-org/llama.cpp/releases?per_page=1` (liste triée par date, et non `/releases/latest` qui pointe vers une release différente sans binaires précompilés) renvoie le release le plus récent.

Le parsing JSON (`extraireAssetsDepuisJson`) isole chaque objet asset par **équilibrage d'accolades** avant d'y chercher `name`/`browser_download_url` : une simple regex de proximité entre ces deux champs échoue, car chaque asset contient un objet `uploader` imbriqué (avec ses propres accolades) entre les deux.

---

## 🔗 Intégration

`LlmEngine.execute` appelle `telechargerEtInstallerRuntime` lors du premier provisioning du modèle (voir [`LlmEngine`](LlmEngine.md)), sous la même autorisation utilisateur que le téléchargement du modèle — pas de second prompt séparé.
