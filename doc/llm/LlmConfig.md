# ⚙️ Documentation : `src/llm/LlmConfig.java`

## 🎯 Rôle du Fichier

Le fichier [`LlmConfig.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/LlmConfig.java) assure la **gestion et la persistance des préférences utilisateur** pour le sous-système d'inférence locale.

Il mémorise notamment le statut de l'accord de téléchargement pour éviter de réinterroger l'utilisateur à chaque exécution.

---

## 📄 Format du Fichier de Configuration (`settings.json`)

Par défaut, la configuration est enregistrée dans `.llm_config/settings.json` :

```json
{
  "permissionAccordee": true,
  "modeleParDefaut": "auto",
  "repertoireModeles": "models",
  "repertoireRuntime": "runtime",
  "temperature": 0.7,
  "maxTokens": 2048,
  "streamingActive": true
}
```

---

## 🔧 Champs de Configuration

| Champ JSON | Type | Valeur par défaut | Description |
| :--- | :--- | :--- | :--- |
| `permissionAccordee` | `boolean` | `false` | Indique si l'utilisateur a accordé l'autorisation d'installer les modèles. |
| `modeleParDefaut` | `String` | `"auto"` | Modèle favori forcé (`auto`, `qwen`, `gemma`, `deepseek`, `smollm`). |
| `repertoireModeles` | `String` | `"models"` | Répertoire local où sont stockés les binaires GGUF. |
| `repertoireRuntime` | `String` | `"runtime"` | Répertoire local où est installé le runtime natif llama-cli (voir [`RuntimeInstaller`](RuntimeInstaller.md)). |
| `temperature` | `double` | `0.7` | Degré de créativité/déterminisme de la génération (0.0 à 1.0). |
| `maxTokens` | `int` | `2048` | Nombre maximum de tokens à générer par réponse. |
| `streamingActive` | `boolean` | `true` | Affichage token par token en temps réel dans la console. |
