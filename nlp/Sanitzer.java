package nlp;

public class Sanitzer {

    // On nettoie le texte pour faciliter la détection des mots-clés
    public static String nettoyerPrompt(String prompt) {
        // Si le prompt est vide ou null on renvoie du vide
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        // On remplace la ponctuation par des espaces pour pas coller les mots entre eux
        String propre = prompt.replaceAll("\\p{Punct}", " ");

        // Tout en minuscules, on retire les doubles espaces et on coupe les bords
        propre = propre.toLowerCase();
        propre = propre.replaceAll("\\s+", " ");
        propre = propre.trim();

        // On renvoie le texte propre
        return propre;
    }
}
