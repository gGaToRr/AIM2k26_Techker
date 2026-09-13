# 📄 Documentation : Mini-Framework de Test TDD (`test/framework/`)

## 📌 Rôle du Framework
Pour garantir une indépendance totale et zéro dépendance externe lourde (comme JUnit ou TestNG), le projet dispose de son propre **mini-framework de test TDD** léger, ultra-rapide et autonome.

---

## 🧩 Les 2 Composants du Framework

### 1. [`test/framework/Assert.java`](file:///home/kaets0ner/Desktop/Project_Ia/test/framework/Assert.java)
Fournit un ensemble complet d'assertions strictes levant une exception `AssertionError` détaillée en cas d'échec :
* `assertTrue(boolean condition, String message)`
* `assertFalse(boolean condition, String message)`
* `assertEquals(Object expected, Object actual, String message)`
* `assertNotEquals(Object unexpected, Object actual, String message)`
* `assertNotNull(Object object, String message)`
* `assertContains(String text, String substring, String message)`
* `assertNotContains(String text, String substring, String message)`
* `assertContainsIgnoreCase(String text, String substring, String message)`
* `assertMatchesRegex(String text, String regexPattern, String message)`
* `assertSize(Collection<?> collection, int expectedSize, String message)`
* `assertBetweenInclusive(double value, double min, double max, String message)`
* `assertStrictPositive(double value, String message)`

### 2. [`test/framework/TestRunner.java`](file:///home/kaets0ner/Desktop/Project_Ia/test/framework/TestRunner.java)
* **Découverte par Réflexion** : Parcourt toutes les méthodes publiques commençant par `test` dans les classes enregistrées.
* **Affichage ANSI Coloré** :
  - `✔ [PASS]` en vert avec le nom de la méthode testée.
  - `✘ [FAIL]` en rouge avec la trace détaillée de l'assertion en échec.
* **Chronométrage Précis** : Mesure le temps d'exécution global en millisecondes.
* **Code de Sortie** : Retourne `true` si 100% des tests réussissent, `false` dès qu'un test échoue (permettant d'interrompre un pipeline CI).

---

## 👨‍💻 Exemple d'Écriture d'un Test TDD

```java
public class MonComposantTest {
    public void testAddition() {
        int resultat = 2 + 2;
        Assert.assertEquals(4, resultat, "2 + 2 doit faire 4");
    }
}
```
