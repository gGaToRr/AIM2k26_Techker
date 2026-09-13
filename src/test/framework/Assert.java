package test.framework;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

// Assertions strictes, précises et exhaustives pour la suite de tests TDD
public class Assert {

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (attendu: true, obtenu: false)");
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (attendu: false, obtenu: true)");
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Attendu: <" + expected + ">\n  Obtenu : <" + actual + ">");
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual, String message) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (valeur inattendue obtenue: <" + actual + ">)");
        }
    }

    public static void assertNotNull(Object object, String message) {
        if (object == null) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (l'objet est null)");
        }
    }

    public static void assertNull(Object object, String message) {
        if (object != null) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (l'objet n'est pas null: " + object + ")");
        }
    }

    public static void assertContains(String text, String substring, String message) {
        if (text == null || !text.contains(substring)) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Texte complet: <" + text + ">\n  Devait contenir: <" + substring + ">");
        }
    }

    public static void assertNotContains(String text, String substring, String message) {
        if (text != null && text.contains(substring)) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Texte complet: <" + text + ">\n  Ne devait PAS contenir: <" + substring + ">");
        }
    }

    public static void assertContainsIgnoreCase(String text, String substring, String message) {
        if (text == null || substring == null || !text.toLowerCase().contains(substring.toLowerCase())) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Texte complet: <" + text + ">\n  Devait contenir (sans casse): <" + substring + ">");
        }
    }

    public static <T> void assertContainsElement(Collection<T> collection, T element, String message) {
        if (collection == null || !collection.contains(element)) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Collection: " + collection + "\n  Devait contenir l'élément: <" + element + ">");
        }
    }

    public static <T> void assertNotContainsElement(Collection<T> collection, T element, String message) {
        if (collection != null && collection.contains(element)) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Collection: " + collection + "\n  Ne devait PAS contenir l'élément: <" + element + ">");
        }
    }

    public static void assertSize(Collection<?> collection, int expectedSize, String message) {
        if (collection == null) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (la collection est null)");
        }
        if (collection.size() != expectedSize) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Taille attendue: <" + expectedSize + ">\n  Taille obtenue : <" + collection.size() + ">");
        }
    }

    public static void assertMatchesRegex(String text, String regex, String message) {
        if (text == null || !Pattern.compile(regex, Pattern.DOTALL).matcher(text).find()) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + "\n  Texte: <" + text + ">\n  Devait matcher le pattern regex: <" + regex + ">");
        }
    }

    public static void assertGreaterThanOrEqual(double min, double actual, String message) {
        if (actual < min) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (attendu >= " + min + ", obtenu: " + actual + ")");
        }
    }

    public static void assertStrictPositive(double actual, String message) {
        if (actual <= 0.0) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (attendu > 0, obtenu: " + actual + ")");
        }
    }

    public static void assertInRange(double min, double max, double actual, String message) {
        if (actual < min || actual > max) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (attendu dans [" + min + ", " + max + "], obtenu: " + actual + ")");
        }
    }

    public static void assertBetweenInclusive(int min, int max, int actual, String message) {
        if (actual < min || actual > max) {
            throw new AssertionError("ÉCHEC ASSERTION: " + message + " (attendu entre " + min + " et " + max + ", obtenu: " + actual + ")");
        }
    }
}
