# 📄 Documentation : `src/nlp/Lemmatizer.java`

## 📌 Rôle du Fichier
`src/nlp/Lemmatizer.java` est le **moteur d'analyse morphologique et de tokenisation**. Il extrait les mots significatifs, filtre les mots vides (stopwords), calcule la densité de code et orchestre la création du `PromptProfile`.

---

## ⚙️ Algorithmes & Traitements

1. **Séparation CamelCase (`normaliserIdentifiants`)** :
   - Transforme `getUserProfileById` en `get user profile by id` avant la mise en minuscules pour isoler les concepts sémantiques.

2. **Expressions Multi-Mots (`EXPRESSIONS_MULTI_MOTS`)** :
   - Détecte les locutions composées comme `pas à pas`, `root cause`, `base de données`, `machine learning`, `tri fusion` et les agrège sous forme de tokens uniques (`pas_a_pas`, `root_cause`).

3. **Filtrage des Mots Vides (`STOPWORDS`)** :
   - Élimine plus de 150 mots fonctionnels en français et anglais (`le`, `la`, `dans`, `avec`, `the`, `with`, `and`...).

4. **Racinisation (Stemming) Heuristique (`raciniser`)** :
   - Rapproche les verbes conjugués et les pluriels de leur forme canonique (ex: `déboguer`, `débogage`, `débogué` → lemme `depann`).

5. **Détection de Code & Langue** :
   - `calculerDensiteCode()` : Mesure la proportion de symboles (`{`, `}`, `(`, `)`, `;`, `=>`, `==`, `def`, `class`, `import`).
   - `detecterLangue()` : Identifie si le prompt est rédigé en français (`FR`) ou en anglais (`EN`).

---

## 🛠️ Méthodes Principales

* `analyser(String rawPrompt)` : Point d'entrée principal qui exécute l'analyse complète et retourne un `PromptProfile`.
* `tokeniser(String text)` : Découpe le texte en liste ordonnée de tokens nettoyés.
* `extraireLemmes(List<String> tokens)` : Calcule les fréquences de chaque lemme racinisé.

---

## 👨‍💻 Exemple d'Utilisation

```java
PromptProfile profil = Lemmatizer.analyser("Déboguer cette boucle infinie en JavaScript");
System.out.println("Tokens : " + profil.tokens());
System.out.println("Langue : " + profil.language());
System.out.println("Technos : " + profil.detectedTechnologies());
```
