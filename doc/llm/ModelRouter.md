# 🧠 Documentation : `llm/ModelRouter.java`

## 🎯 Rôle du Fichier

Le composant [`ModelRouter.java`](file:///home/kaets0ner/Desktop/Project_Ia/llm/ModelRouter.java) implémente le **moteur de décision et d'aiguillage sémantique**. Il analyse la fiche d'identité du prompt ([`PromptProfile`](file:///home/kaets0ner/Desktop/Project_Ia/nlp/PromptProfile.java)) produite par le pipeline NLP et sélectionne de manière déterministe le modèle de langage léger le plus apte à traiter la demande.

---

## ⚙️ Logique Décisionnelle en Cascade

```mermaid
flowchart TD
    Profile["PromptProfile (NLP)"] --> CodeCheck{"Densité Code >= 15% ou<br/>Stack Technique Détectée ?"}
    
    CodeCheck -->|OUI| Qwen["💻 Qwen 2.5 Coder (1.5B)<br/>Confiance: 95%"]
    CodeCheck -->|NON| ArchetypeCheck{"Archétype Principal"}
    
    ArchetypeCheck -->|"ARCHITECTURE ou DÉPANNAGE"| Qwen
    ArchetypeCheck -->|"COMPARAISON ou PROTOCOLE"| DeepSeek["🧠 DeepSeek R1 (1.5B)<br/>Confiance: 92%"]
    ArchetypeCheck -->|"Question Courte (< 12 mots)"| Smol["⚡ SmolLM2 (1.7B)<br/>Confiance: 85%"]
    ArchetypeCheck -->|"TUTORIEL / VULGARISATION / RÉDACTION"| Gemma["✍️ Gemma 2 (2B)<br/>Confiance: 88%"]
```

---

## 📊 Structure du Résultat (`RoutingDecision`)

Chaque décision retourne un record Java contenant :
- `selectedModel` : Le [`ModelType`](file:///home/kaets0ner/Desktop/Project_Ia/llm/ModelType.java) choisi.
- `rationale` : L'explication textuelle pédagogique affichée à l'utilisateur.
- `confidenceScore` : Le score de confiance de l'attribution (ex: `0.95`).
- `isManualOverride` : Booléen indiquant si le modèle a été imposé manuellement via `--model / -m`.
