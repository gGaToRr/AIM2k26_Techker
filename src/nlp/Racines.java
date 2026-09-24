package nlp;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Ramene les mots a une racine commune pour les comparer : "chat", "chats", "chaton",
// "Chatons" -> "chat". Sans dictionnaire : accents et casse retires, puis terminaisons
// courantes (pluriels, feminins, diminutifs, formes verbales anglaises).
// Utilise par la recherche dans le corpus et par la detection des themes.
public final class Racines {

    // "eau" n'en fait pas partie : "chateau" ne doit pas devenir "chat"
    private static final List<String> TERMINAISONS = List.of(
            "ettes", "ette", "onnes", "onne", "ons", "on", "ing", "ers", "es", "ed", "er", "s", "x", "e");
    public static final int LONGUEUR_MIN = 3;
    // Mots dont la terminaison n'est pas une marque de pluriel : "mois" ne doit pas devenir "moi"
    private static final Set<String> INVARIABLES = Set.of(
            "mois", "fois", "pays", "temps", "corps", "bras", "cours", "sens", "avis", "souris", "repas",
            "succes", "proces", "acces", "progres", "prix", "choix", "voix", "noix", "deux", "trois", "jus",
            "news", "this", "was", "has", "his", "its", "yes", "plus", "gas", "bus", "virus", "campus");
    private static final Pattern MOT = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern DIACRITIQUES = Pattern.compile("\\p{M}");
    // "chat gpt" en deux mots (ou mal orthographie : "chat gtp") n'est pas un chat
    private static final Pattern CHAT_GPT = Pattern.compile("chat[\\s-]*(gpt|gtp|gbt)");

    private Racines() {}

    // Racines distinctes d'un texte, dans l'ordre d'apparition
    public static Set<String> de(String texte) {
        Set<String> racines = new LinkedHashSet<>();
        Matcher m = MOT.matcher(CHAT_GPT.matcher(normaliser(texte)).replaceAll("chatgpt"));
        while (m.find()) {
            String racine = racineNormalisee(m.group());
            if (racine.length() >= LONGUEUR_MIN) racines.add(racine);
        }
        return racines;
    }

    public static String racine(String mot) {
        return racineNormalisee(normaliser(mot));
    }

    // Terminaisons anglaises : retirees seulement s'il reste un vrai radical ("spring" ne doit
    // pas devenir "spr", ni "string" "str")
    private static final Set<String> TERMINAISONS_ANGLAISES = Set.of("ing", "ers", "ed", "er");
    private static final int RADICAL_ANGLAIS_MIN = 4;

    private static String racineNormalisee(String minuscule) {
        if (INVARIABLES.contains(minuscule)) return minuscule;
        // Feminin en -ienne(s) : "vegetariennes" -> "vegetarien", comme le masculin
        if (minuscule.endsWith("iennes")) minuscule = minuscule.substring(0, minuscule.length() - 3);
        else if (minuscule.endsWith("ienne")) minuscule = minuscule.substring(0, minuscule.length() - 2);
        for (String fin : TERMINAISONS) {
            int minimum = TERMINAISONS_ANGLAISES.contains(fin) ? RADICAL_ANGLAIS_MIN : LONGUEUR_MIN;
            if (minuscule.endsWith(fin) && minuscule.length() - fin.length() >= minimum) {
                return minuscule.substring(0, minuscule.length() - fin.length());
            }
        }
        return minuscule;
    }

    // Sans accents, en minuscules
    public static String normaliser(String texte) {
        return DIACRITIQUES.matcher(Normalizer.normalize(texte == null ? "" : texte, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
    }
}
