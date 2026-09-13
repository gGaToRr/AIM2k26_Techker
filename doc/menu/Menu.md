# 📄 Documentation : `menu/Menu.java`

## 📌 Rôle du Fichier
`menu/Menu.java` gère l'interface textuelle interactive de l'application lorsqu'aucun argument direct (`-i`, `-f`) n'est passé en ligne de commande.

---

## 🎯 Problématiques Résolues
1. **Collage de Blocs de Code** : Lorsqu'un utilisateur colle du code contenant des lignes vides, des accolades ou des deux-points (`:`), la saisie ne doit pas s'interrompre prématurément.
2. **Sentinelles de Fin** : Permet la validation explicite via la commande `:done` ou `:end` (insensible à la casse).
3. **Piping Unix** : Détecte si des données arrivent via redirection de flux (`cat prompt.txt | java Main`) en analysant `System.in.available()`.

---

## 🛠️ Méthodes Principales

* `afficherBienvenue()` : Affiche la bannière d'accueil ASCII encadrée du projet.
* `demanderPrompt()` : Lit la saisie de l'utilisateur ligne par ligne en accumulant le texte dans un `StringBuilder` jusqu'à validation.
* `afficherResultat(String message)` : Affiche un message d'information ou de diagnostic.
* `fermer()` : Ferme proprement le `Scanner` sous-jacent.

---

## 👨‍💻 Exemple d'Utilisation

```java
Menu menu = new Menu();
menu.afficherBienvenue();
String prompt = menu.demanderPrompt();
System.out.println("Prompt saisi : " + prompt);
menu.fermer();
```
