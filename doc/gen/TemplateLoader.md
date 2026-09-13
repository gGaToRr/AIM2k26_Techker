# 📄 Documentation : `gen/TemplateLoader.java`

## 📌 Rôle du Fichier
`gen/TemplateLoader.java` est responsable de la lecture, de la mise en cache mémoire et de la résolution des fichiers de templates Markdown situés dans le dossier `genPrompt/`.

---

## ⚙️ Fonctionnement

1. **Organisation par Dossiers Thématiques** :
   - `learning/` : Templates d'apprentissage et cours.
   - `architecture/` : Templates de conception logicielle et code.
   - `troubleshooting/` : Templates de débug, audit et refactoring.
   - `creation/` : Templates de rédaction, traduction et brainstorming.
   - `protocol/` : Templates de recettes et procédures.
   - `comparison/` : Matrices comparatives et aide à la décision.
   - `concept/` : Vulgarisation et guides encyclopédiques.

2. **Cache Mémoire Haute Performance (`CACHE_TEMPLATES`)** :
   - Chaque template lu sur le disque est mis en mémoire dans une `Map<String, String>`. Les lectures ultérieures s'exécutent en **0.001 ms**.

3. **Recherche Souple & Forçage (`chargerTemplateParNom`)** :
   - Recherche par chemin direct (`genPrompt/learning/guide_debutant.md`).
   - Recherche par nom simple (`guide_debutant`, `feynman`, `recette_culinaire`).
   - Recherche insensible à la casse et par correspondance partielle.

4. **Fallback Universel Sécurisé (`fallbackTemplate`)** :
   - Si un fichier de template venait à être supprimé ou corrompu sur disque, un template Markdown générique et robuste est automatiquement généré à la volée.

---

## 🛠️ Méthodes Principales

* `chargerTemplate(TypeOfPrompt type, String subType)` : Charge le template associé à un archétype et sous-type.
* `chargerTemplateParNom(String nom)` : Recherche un template par son nom ou chemin (utilisé pour le flag `-t` / `--template`).

---

## 👨‍💻 Exemple d'Utilisation

```java
String template = TemplateLoader.chargerTemplate(TypeOfPrompt.CONCEPTION_ARCHITECTURE, "code_generation");
System.out.println("Taille du template : " + template.length() + " caractères");
```
