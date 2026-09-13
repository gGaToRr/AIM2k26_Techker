# 📋 Documentation : `src/llm/ModelType.java`

## 🎯 Rôle du Fichier

L'énumération [`ModelType.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/ModelType.java) définit le registre central des **Small Language Models (SLMs)** légers supportés pour l'inférence 100% locale.

Chaque entrée encapsule l'ensemble des métadonnées nécessaires au routage, au téléchargement et à l'exécution :
- Identifiant unique et alias acceptés en ligne de commande.
- Spécialité et description pédagogique pour débutants.
- Poids disque (format quantifié GGUF Q4_K_M) et mémoire RAM minimale requise.
- URL de téléchargement officiel et nom de fichier cible.
- Liste des archétypes cibles du moteur NLP.

---

## 🤖 Tableau des Modèles Supportés

| Constante Enum | Nom Commercial | Spécialité / Rôle | Poids GGUF | RAM Min. | Archétypes Cibles |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `QWEN_CODER` | **Qwen 2.5 Coder (1.5B)** | Code, Débogage, Architecture | ~1.1 Go | ~1.5 Go | `CONCEPTION_ARCHITECTURE`, `DEPANNAGE_DIAGNOSTIC` |
| `GEMMA_GENERAL` | **Gemma 2 (2B)** | Français, Pédagogie, Rédaction | ~1.6 Go | ~2.0 Go | `APPRENTISSAGE_TUTORIEL`, `CONCEPT_VULGARISATION`, `CREATION_REDACTION` |
| `DEEPSEEK_REASONING`| **DeepSeek R1 Distill (1.5B)** | Raisonnement logique, Comparatif | ~1.1 Go | ~1.5 Go | `COMPARAISON_DECISION`, `PROTOCOLE_RECETTE` |
| `SMOLLM_FAST` | **SmolLM2 (1.7B)** | Synthèse rapide, Faible empreinte | ~1.0 Go | ~1.2 Go | Prompts courts, questions directes |

---

## 🔍 Résolution Dynamique d'Alias (`fromAlias`)

La méthode `fromAlias(String alias)` permet une saisie tolérante et intuitive pour l'utilisateur :
- `"qwen"`, `"coder"`, `"code"`, `"dev"` $\rightarrow$ `QWEN_CODER`
- `"gemma"`, `"google"`, `"general"`, `"redaction"` $\rightarrow$ `GEMMA_GENERAL`
- `"deepseek"`, `"r1"`, `"reasoning"`, `"logique"` $\rightarrow$ `DEEPSEEK_REASONING`
- `"smollm"`, `"fast"`, `"rapide"`, `"light"` $\rightarrow$ `SMOLLM_FAST`
