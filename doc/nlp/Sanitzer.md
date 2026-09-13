# 📄 Documentation : `nlp/Sanitzer.java`

## 📌 Rôle du Fichier
`nlp/Sanitzer.java` prépare et normalise le texte brut. Il résout les fautes de frappe d'élision courantes (ex: `cest`, `lavion`, `quon`, `dargent`) et nettoie la ponctuation tout en protégeant les symboles technologiques essentiels.

---

## 🎯 Fonctionnalités Clés

1. **Restauration des Élisions Françaises (`DICTIONNAIRE_ELISIONS`)** :
   - Formes verbales : `cest` → `c'est`, `jai` → `j'ai`, `yatil` → `y a-t-il`, `peutetre` → `peut-être`.
   - Articles et pronoms : `lavion` → `l'avion`, `lunivers` → `l'univers`, `dargent` → `d'argent`, `quon` → `qu'on`.
   - Conjonctions : `jusqua` → `jusqu'à`, `puisquil` → `puisqu'il`.

2. **Normalisation Typographique** :
   - Convertit les apostrophes exotiques (`’`, `‘`, `` ` ``) en apostrophe simple standard `'`.

3. **Protection des Symboles Critiques** :
   - Préserve les symboles de code comme `C++`, `C#`, `@annotation`, `#hashtag`.

4. **Suppression des Accents (`supprimerAccents`)** :
   - Normalise les caractères accentués via `java.text.Normalizer` (`Médecine` → `Medecine`) pour les comparaisons insensibles aux accents.

---

## 🛠️ Méthodes Principales

* `restaurerElisions(String text)` : Applique le dictionnaire de règles d'élisions et de contractions.
* `nettoyerPrompt(String prompt)` : Prépare le texte pour la tokenisation NLP (minuscules, espaces normalisés).
* `nettoyerEtFormaterTexte(String text)` : Formate le texte proprement pour l'affichage avec majuscule en début de phrase.
* `supprimerAccents(String texte)` : Élimine les diacritiques et accents.

---

## 👨‍💻 Exemple d'Utilisation

```java
String brut = "cest lavion dargent";
String propre = Sanitzer.restaurerElisions(brut);
System.out.println(propre); // "c'est l'avion d'argent"
```
