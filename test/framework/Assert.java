package test.framework;

import java.util.Collection;
import java.util.Objects;

// Assertions légères et explicites pour les tests unitaires TDD
public class Assert {

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ÉCHEC: " + message + " (attendu: true, obtenu: false)");
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("ÉCHEC: " + message + " (attendu: false, obtenu: true)");
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("ÉCHEC: " + message + "\n  Attendu: <" + expected + ">\n  Obtenu : <" + actual + ">");
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual, String message) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError("ÉCHEC: " + message + " (valeur inattendue obtenue: <" + actual + ">)");
        }
    }

    public static void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new AssertionError("ÉCHEC: " + message + " (l'objet est null)");
        }
    }

    public static void assertContains(String text, String substring, String message) {
        if (text == null || !text.contains(substring)) {
            throw new AssertionError("ÉCHEC: " + message + "\n  Texte complet: <" + text + ">\n  Devait contenir: <" + substring + ">");
        }
    }

    public static void assertContainsIgnoreCase(String text, String substring, String message) {
        if (text == null || substring == null || !text.toLowerCase().contains(substring.toLowerCase())) {
            throw new AssertionError("ÉCHEC: " + message + "\n  Texte complet: <" + text + ">\n  Devait contenir (sans casse): <" + substring + ">");
        }
    }

    public static <T> void assertContainsElement(Collection<T> collection, T element, String message) {
        if (collection == null || !collection.contains(element)) {
            throw new AssertionError("ÉCHEC: " + message + "\n  Collection: " + collection + "\n  Devait contenir l'élément: <" + element + ">");
        }
    }

    public static void assertGreaterThanOrEqual(double min, double actual, String message) {
        if (actual < min) {
            throw new AssertionError("ÉCHEC: " + message + " (attendu >= " + min + ", obtenu: " + actual + ")");
        }
    }

    public static void assertInRange(double min, double max, double actual, String message) {
        if (actual < min || actual > max) {
            throw new AssertionError("ÉCHEC: " + message + " (attendu dans [" + min + ", " + max + "], obtenu: " + actual + ")");
        }
    }
}
