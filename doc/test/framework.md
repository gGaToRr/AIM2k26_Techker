# 🧪 Documentation : `src/test/framework/`

## 📌 Rôle

Framework de test maison, sans dépendance externe. `TestRunner` instancie les classes enregistrées, exécute leurs cas et produit un rapport ANSI.

---

## 🏷️ Sélection des cas : `@Test`

La détection se faisait par préfixe de nom (`testXxx`). Une méthode utilitaire nommée ainsi par inadvertance était donc exécutée comme un test.

```java
@Test
public void testEmpreinteInvalideEstRejetee() { … }
```

**Règle de sélection** : l'annotation `@Test` fait autorité dès qu'elle est présente quelque part dans la classe. Si une classe n'en contient aucune, `TestRunner` retombe sur l'ancienne convention de préfixe.

Ce repli n'est pas de la complaisance : il permet d'ajouter une classe de test sans migration préalable, et il a permis de migrer les 154 méthodes existantes sans fenêtre où la suite aurait été rouge.

| Classe | Méthodes retenues |
|:---|:---|
| Contient au moins un `@Test` | Uniquement les méthodes annotées |
| N'en contient aucun | Les méthodes `public void testXxx()` |

---

## 🔁 `@BeforeEach` / `@AfterEach`

```java
public class MonTest {
    private Path dossierTemporaire;

    @BeforeEach
    public void preparer() throws Exception {
        dossierTemporaire = Files.createTempDirectory("cas");
    }

    @AfterEach
    public void nettoyer() throws Exception {
        Files.deleteIfExists(dossierTemporaire);
    }
}
```

Les méthodes nommées `setUp()` et `tearDown()` sont reconnues sans annotation.

Deux garanties :

- Les hooks encadrent **chaque cas**, pas la classe.
- `@AfterEach` s'exécute **même si le cas a échoué** — un nettoyage qui ne s'exécute qu'en cas de succès laisse des fichiers temporaires derrière lui précisément quand le test échoue.

---

## 🧼 Isolation

`TestRunner` crée **une instance neuve par cas de test**. Auparavant, une seule instance était partagée par toute la classe : un champ modifié par un test était vu par le suivant, et l'ordre d'exécution devenait significatif.

---

## 🔢 Ordre déterministe

`Class.getDeclaredMethods()` ne garantit aucun ordre — il peut varier d'une JVM ou d'une compilation à l'autre. Les cas sont donc **triés par nom**, pour que deux exécutions successives produisent exactement le même rapport.
