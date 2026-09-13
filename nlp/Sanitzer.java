package nlp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Nettoie le texte, restaure les élisions et répare les contractions orthographiques courantes
public class Sanitzer {

    // Normalisation des apostrophes typographiques (’ ‘ `) vers l'apostrophe standard (')
    private static final Pattern APOSTROPHES_TYPO = Pattern.compile("[’‘`]");

    // Liste ordonnée des règles de restauration d'élisions et contractions (terme erroné -> forme correcte)
    private static final Map<String, String> DICTIONNAIRE_ELISIONS = new LinkedHashMap<>();

    static {
        // Formes verbales et expressions composées (prioritaires sur les mots isolés)
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(yatil|ya\\s+t\\s+il|ya\\s+til|y\\s+a\\s+t\\s+il)\\b", "y a-t-il");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(ya|ilya)\\b", "il y a");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(atil|a\\s+til|a\\s+t\\s+il)\\b", "a-t-il");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(atelle|a\\s+telle|a\\s+t\\s+elle)\\b", "a-t-elle");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bj['\\s]*ai\\s+besoins?\\b", "j'ai besoin");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(sil\\s+vous\\s+pla[iî]t|s\\s+il\\s+vous\\s+pla[iî]t)\\b", "s'il vous plaît");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(sil\\s+te\\s+pla[iî]t|s\\s+il\\s+te\\s+pla[iî]t)\\b", "s'il te plaît");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(aujourdhui)\\b", "aujourd'hui");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(quelquun)\\b", "quelqu'un");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(quelquune)\\b", "quelqu'une");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(peut\\s+etre|peutetre)\\b", "peut-être");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(parceque|parce\\s+que)\\b", "parce que");

        // Formes verbales contractées avec pronom
        DICTIONNAIRE_ELISIONS.put("(?i)\\bcest\\b", "c'est");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(cetait|cetais)\\b", "c'était");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bsest\\b", "s'est");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(setait|setais)\\b", "s'était");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bnest\\b", "n'est");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(netait|netais)\\b", "n'était");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bjai\\b", "j'ai");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bjavais\\b", "j'avais");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bjaurais\\b", "j'aurais");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bjaime\\b", "j'aime");

        // Conjonctions et pronoms relatifs
        DICTIONNAIRE_ELISIONS.put("(?i)\\bquun\\b", "qu'un");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bquune\\b", "qu'une");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bquil\\b", "qu'il");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bquils\\b", "qu'ils");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bquon\\b", "qu'on");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(lorsquil)\\b", "lorsqu'il");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(lorsquon)\\b", "lorsqu'on");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(lorsquun)\\b", "lorsqu'un");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(puisquil)\\b", "puisqu'il");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(puisquon)\\b", "puisqu'on");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(puisquun)\\b", "puisqu'un");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(jusqua|jusqu\\s+a)\\b", "jusqu'à");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(jusquau)\\b", "jusqu'au");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(jusquaux)\\b", "jusqu'aux");
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(jusquici)\\b", "jusqu'ici");

        // Articles contractés manquants avec prépositions (d'...)
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdun\\b", "d'un");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdune\\b", "d'une");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdargent\\b", "d'argent");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdautre\\b", "d'autre");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdautres\\b", "d'autres");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdaccord\\b", "d'accord");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdavance\\b", "d'avance");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdaide\\b", "d'aide");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdidee\\b", "d'idée");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdimpact\\b", "d'impact");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdexemple\\b", "d'exemple");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdexemples\\b", "d'exemples");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdexercice\\b", "d'exercice");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bdexercices\\b", "d'exercices");

        // Noms courants avec article élidé soudé (l'...)
        DICTIONNAIRE_ELISIONS.put("(?i)\\b(lavion|lavions)\\b", "l'avion");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blart\\b", "l'art");
        DICTIONNAIRE_ELISIONS.put("(?i)\\bleau\\b", "l'eau");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blair\\b", "l'air");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blimage\\b", "l'image");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blidee\\b", "l'idée");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blimpact\\b", "l'impact");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blunivers\\b", "l'univers");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blhomme\\b", "l'homme");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blhumain\\b", "l'humain");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blordinateur\\b", "l'ordinateur");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blaide\\b", "l'aide");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blexemple\\b", "l'exemple");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blexplication\\b", "l'explication");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blutilisateur\\b", "l'utilisateur");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blapplication\\b", "l'application");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blarchitecture\\b", "l'architecture");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blarborescence\\b", "l'arborescence");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blalgorithme\\b", "l'algorithme");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blapprentissage\\b", "l'apprentissage");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blhistoire\\b", "l'histoire");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blenvironnement\\b", "l'environnement");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blun\\b", "l'un");
        DICTIONNAIRE_ELISIONS.put("(?i)\\blune\\b", "l'une");

        // Espaces parasites entre pronom élidé et voyelle (ex: "l avion" -> "l'avion", "d un" -> "d'un")
        DICTIONNAIRE_ELISIONS.put("(?i)\\b([ldcjsmnt]|qu)\\s+([aeiouyhéèêëàâîïôûù])", "$1'$2");
    }

    // Restaure les élisions contractées et les apostrophes manquantes
    public static String restaurerElisions(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String resultat = APOSTROPHES_TYPO.matcher(text).replaceAll("'");

        for (Map.Entry<String, String> entry : DICTIONNAIRE_ELISIONS.entrySet()) {
            resultat = resultat.replaceAll(entry.getKey(), entry.getValue());
        }

        return resultat;
    }

    // Capitalise le début du texte et après les points
    public static String capitaliserPhrases(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (capitalizeNext && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }

            if (c == '.' || c == '!' || c == '?') {
                capitalizeNext = true;
            }
        }

        return sb.toString();
    }

    // Nettoie et formate le texte pour un rendu soigné
    public static String nettoyerEtFormaterTexte(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String restaure = restaurerElisions(text.trim());
        restaure = restaure.replaceAll("\\s+", " ");
        return capitaliserPhrases(restaure);
    }

    // Nettoie le texte pour l'analyse NLP (retrait ponctuation, minuscules)
    public static String nettoyerPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        // On restaure d'abord les élisions pour séparer les tokens agglutinés (ex: lavion -> l'avion -> l avion)
        String restaure = restaurerElisions(prompt);

        // Remplacement de la ponctuation et apostrophes par des espaces
        String propre = restaure.replaceAll("[\\p{Punct}&&[^+#]]", " ");

        // Tout en minuscules, normalisation des espaces
        propre = propre.toLowerCase();
        propre = propre.replaceAll("\\s+", " ");
        propre = propre.trim();

        return propre;
    }
}
