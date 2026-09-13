# ⚡ Documentation : `llm/LlmEngine.java` & `LocalLlmBackend.java`

## 🎯 Rôle du Fichier

[`LlmEngine.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/LlmEngine.java) est **l'orchestrateur d'exécution de haut niveau**. Il coordonne :
1. La décision du [`ModelRouter`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelRouter.md).
2. La vérification / onboarding via [`ModelInstaller`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelInstaller.md).
3. L'exécution de l'inférence via [`LocalLlmBackend`](file:///home/kaets0ner/Desktop/Project_Ia/llm/LocalLlmBackend.java).
4. Le streaming en temps réel et le calcul des statistiques de performances.

---

## 🏎️ Double Mode d'Exécution de `LocalLlmBackend`

```mermaid
flowchart TD
    Req["Demande d'Inférence"] --> RunnerCheck{"Binaire llama-cli<br/>détecté dans le système ?"}
    
    RunnerCheck -->|OUI| Native["🚀 Inférence Native Hardware (CPU/GPU)<br/>via llama-cli avec modèle quantifié GGUF"]
    RunnerCheck -->|NON| Standalone["🛡️ Mode Inférence Autonome<br/>Génération structurée directe sans dépendance"]
    
    Native --> Stream["Streaming des Tokens dans la Console"]
    Standalone --> Stream
    
    Stream --> Metrics["Calcul du Débit (tokens/sec) et Latence (ms)"]
```

---

## 📊 Métriques de Performance Rapportées

Après chaque génération, un bandeau statistique résume l'exécution :
```
📊 [Performance] 142 tokens générés en 2850 ms (49.8 tokens/sec) via Qwen 2.5 Coder (1.5B Instruct)
```
