# 📄 Documentation : `gen/MetaPromptEngine.java`

## 📌 Rôle du Fichier
`gen/MetaPromptEngine.java` est le **moteur d'orchestration Meta-Prompting**. Il construit le dictionnaire de contexte, applique les 5 règles d'optimisation de prompt, compile le template Mustache et applique les adaptations ciblées pour les différents modèles d'IA.

---

## 🌟 Les 5 Règles d'Optimisation Appliquées

1. **Extraction de la Mission Pure (`extraireMissionPure`)** : Élimine le bruit conversationnel de début et fin (*"Bonjour, est-ce que tu peux..."*, *"merci beaucoup et c'est tout"*).
2. **Injection du Persona d'Expertise & Technologies** : Injecte le rôle spécialisé (`domainPersona`) et la stack technique pour contraindre le style du LLM.
3. **Structuration Séquentielle** : Injecte les sous-objectifs ordonnés (`objectives`) dans des balises XML claires.
4. **Auto-Correction des Faiblesses** : Si le prompt utilisateur est trop court ou manque de contraintes, injecte automatiquement des exigences de structure (ex: *structure aérée avec titres Markdown*, *couvrir les aspects théoriques et pratiques*).
5. **Adaptation par Modèle d'IA (`adapterPourAgent`)** :
   - `claude` : Ajoute l'encadrement `<claude_system_prompt>` et les directives de réflexion `<thinking>`.
   - `deepseek` : Ajoute les consignes de raisonnement pas à pas dans des balises `<think>`.
   - `gpt` : Ajoute les directives de style direct et rigoureux OpenAI GPT-4o.
   - `gemini` : Ajoute les directives de structure Google Gemini.
   - `llama` : Ajoute les balises de conversation officielles LLaMA-3 (`<|start_header_id|>`).
   - `feynman` : Impose la méthode pédagogique Feynman (explication comme à un débutant de 12 ans).

---

## 🛠️ Méthodes Principales

* `genererPromptOptimise(PromptProfile profile)` : Génère le prompt optimisé standard.
* `genererPromptOptimise(PromptProfile profile, CliArgs options)` : Génère le prompt en appliquant les surcharges d'arguments CLI (`-t`, `-d`, `-l`, `-a`).
* `genererExportJson(PromptProfile profile, String promptOptimise, CliArgs options)` : Produit l'objet JSON complet avec profil NLP et prompt généré.
* `adapterPourAgent(String prompt, String agent)` : Enveloppe le prompt avec les conventions du modèle cible.

---

## 👨‍💻 Exemple d'Utilisation

```java
PromptProfile profile = Lemmatizer.analyser("Explique la théorie des graphes");
CliArgs options = CliParser.parse(new String[]{"-a", "claude"});

String superPrompt = MetaPromptEngine.genererPromptOptimise(profile, options);
System.out.println(superPrompt);
```
