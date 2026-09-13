# 🤖 Module LLM - Inférence Locale & Routage Intelligent

## 📖 Présentation Générale

Le module `llm/` apporte au projet la capacité d'**exécuter directement et localement les méta-prompts générés** sur des modèles de langage légers (*Small Language Models* ou SLMs) sans recourir à des services cloud externes (OpenAI, Anthropic, Google).

L'architecture repose sur le concept de **"System-Level Mixture of Specialists" (MoS)** : au lieu d'utiliser un modèle généraliste lourd et consommateur en ressources, l'outil analyse la nature sémantique du prompt via le pipeline NLP et aiguille la requête vers le modèle compact expert le plus compétent (1.0 Go à 1.6 Go).

---

## 🛡️ Principes Directeurs pour les Débutants

1. **Confidentialité Totale (100% Hors-ligne)** : Aucune donnée, aucun code source et aucun texte ne quitte la machine de l'utilisateur.
2. **100% Gratuit & Sans Clé API** : Aucun abonnement ni inscription à des plateformes tierces.
3. **Consentement Explicite & Bienveillant** : Au premier démarrage, un assistant pédagogique détaillé explique clairement les prérequis d'espace disque et demande l'accord de l'utilisateur avant tout téléchargement.
4. **Zéro Configuration Requise** : L'outil gère automatiquement le routage, le téléchargement, le chargement en mémoire et l'inférence.

---

## 🗂️ Sommaire des Composants du Module

- [`ModelType.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelType.md) : Registre des 4 modèles SLM experts (Qwen Coder, Gemma 2, DeepSeek R1, SmolLM2).
- [`ModelRouter.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelRouter.md) : Algorithme de routage automatique basé sur le profil NLP.
- [`ModelInstaller.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/ModelInstaller.md) : Assistant d'onboarding pour débutants et gestionnaire de téléchargement.
- [`LlmConfig.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/LlmConfig.md) : Gestion de la configuration persistante et des autorisations locales.
- [`LlmEngine.md`](file:///home/kaets0ner/Desktop/Project_Ia/doc/llm/LlmEngine.md) : Moteur d'inférence, streaming de tokens et métriques de performance.

---

## 🚀 Utilisation en Ligne de Commande

```bash
# 1. Exécution avec sélection automatique du modèle idéal
java -cp "bin:lib/*" Main -i "Explique le fonctionnement du Garbage Collector en Java" -e

# 2. Exécution avec forçage explicite d'un modèle expert
java -cp "bin:lib/*" Main -i "Trouve la fuite mémoire" -c "Main.java" -e -m qwen
java -cp "bin:lib/*" Main -i "Compare Kafka et RabbitMQ" -e -m deepseek
```
