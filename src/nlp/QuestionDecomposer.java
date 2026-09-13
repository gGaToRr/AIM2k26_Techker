package nlp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Décompose les flux de pensée continus et les requêtes complexes en sous-objectifs ordonnés
public class QuestionDecomposer {

    public record DecompositionResult(
            boolean hasMultipleObjectives,
            List<String> objectives,
            String xmlFormattedList
    ) {}

    // Marqueurs de découpage logique, temporel et séquentiel
    private static final Pattern CONNECTEURS_DECOUPAGE = Pattern.compile(
            "(?i)(?<=[.?!;•\\n\\r])\\s+" +
            "|\\s*\\b(?:et\\s+ensuite|ensuite|et\\s+puis|puis|et\\s+apres|apres|apres\\s+ca|dans\\s+un\\s+second\\s+temps|deuxiemement)\\b\\s*" +
            "|\\s*\\b(?:et\\s+aussi|aussi|de\\s+plus|en\\s+plus|ainsi\\s+que|egalement)\\b\\s*" +
            "|\\s*\\b(?:enfin|pour\\s+finir|pour\\s+terminer|a\\s+la\\s+fin|en\\s+dernier|finalement)\\b\\s*" +
            "|\\s*\\b(?:et\\s+comment\\s+on\\s+fait\\s+pour|et\\s+comment\\s+faire\\s+pour|comment\\s+on\\s+fait\\s+pour|comment\\s+faire\\s+pour|et\\s+comment)\\b\\s*" +
            "|\\s*\\b(?:et\\s+pourquoi\\s+on|et\\s+pourquoi|pourquoi\\s+on)\\b\\s*" +
            "|\\s*\\b(?:et\\s+si\\s+jamais|si\\s+jamais|dans\\s+le\\s+cas\\s+ou)\\b\\s*" +
            "|\\s*\\b(?:je\\s+veux\\s+aussi\\s+savoir|dis\\s+moi\\s+aussi|explique\\s+moi\\s+aussi|n['\\s]*oublie\\s+pas\\s+de)\\b\\s*"
    );

    // Mappage de verbes conversationnels vers formes d'action infinitives
    private static final Map<String, String> VERBES_ACTION_NORMALISES = new LinkedHashMap<>();

    static {
        VERBES_ACTION_NORMALISES.put("(?i)^(?:je\\s+veux\\s+savoir\\s+comment|dis\\s+moi\\s+comment|explique\\s+moi\\s+comment)[,\\s]+", "Comment ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:je\\s+veux\\s+savoir|je\\s+voudrais\\s+savoir|dis\\s+moi|explique\\s+moi|montre\\s+moi|apprends\\s+moi)[,\\s]+", "");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:cree(?:\\s+moi)?|creer)\\s+", "Créer ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:ajoute(?:\\s+moi)?|ajouter)\\s+", "Ajouter ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:genere(?:\\s+moi)?|generer)\\s+", "Générer ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:ecris(?:\\s+moi)?|ecrire)\\s+", "Écrire ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:fais(?:\\s+moi)?|faire)\\s+", "Faire ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:optimise(?:\\s+moi)?|optimiser)\\s+", "Optimiser ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:configure(?:\\s+moi)?|configurer)\\s+", "Configurer ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:integre(?:\\s+moi)?|integrer)\\s+", "Intégrer ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:teste(?:\\s+moi)?|tester)\\s+", "Tester ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:corrige(?:\\s+moi)?|corriger)\\s+", "Corriger ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:refactore(?:\\s+moi)?|refactorer)\\s+", "Refactorer ");
        VERBES_ACTION_NORMALISES.put("(?i)^(?:traduis(?:\\s+moi)?|traduire)\\s+", "Traduire ");
    }

    // Préfixes parasites résiduels
    private static final List<String> PREFIXES_PARASITES = List.of(
            "(?i)^[\\s,;.:\\-•]+",
            "(?i)^(?:salut|bonjour|hello|hey|alors|deja|tout d abord|au debut|premierement|en premier|en premier lieu)[,\\s]+",
            "(?i)^(?:et|puis|ensuite|apres|aussi|enfin|pour|de plus|egalement)[,\\s]+",
            "(?i)^(?:comment on fait pour|comment faire pour|comment on peut)[,\\s]+"
    );

    // Suffixes parasites à retirer en fin de sous-proposition
    private static final String SUFFIXES_PARASITES = "(?i)[,\\s]+(?:merci|merci d avance|merci beaucoup|stp|svp|s il te plait|s'il te plaît|s'il vous plaît|facilement|rapidement|xd)[.!?\\s]*$";

    // Décompose une requête utilisateur brute ou nettoyée en une liste d'objectifs actionnables
    public static DecompositionResult decomposer(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new DecompositionResult(false, List.of(), "");
        }

        String texteNettoye = Sanitzer.restaurerElisions(rawText.trim());

        // Si le texte est très court (moins de 5 mots), pas de décomposition nécessaire
        if (texteNettoye.split("\\s+").length <= 4) {
            return new DecompositionResult(false, List.of(Sanitzer.capitaliserPhrases(texteNettoye)), "");
        }

        String[] segmentsBruts = CONNECTEURS_DECOUPAGE.split(texteNettoye);
        List<String> objectifsNettoyes = new ArrayList<>();

        for (String segment : segmentsBruts) {
            String clean = formaterSegmentEnAction(segment);
            if (!clean.isBlank() && clean.length() >= 4 && !estBruitPur(clean)) {
                objectifsNettoyes.add(clean);
            }
        }

        // Si on a identifié au moins 2 sous-objectifs distincts
        if (objectifsNettoyes.size() >= 2) {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<objectifs_specifiques>\n");
            for (int i = 0; i < objectifsNettoyes.size(); i++) {
                xmlBuilder.append(String.format("  %d. %s\n", i + 1, objectifsNettoyes.get(i)));
            }
            xmlBuilder.append("</objectifs_specifiques>");

            return new DecompositionResult(true, Collections.unmodifiableList(objectifsNettoyes), xmlBuilder.toString());
        }

        // Si 0 ou 1 seul objectif extrait
        String unique = objectifsNettoyes.isEmpty() ? Sanitzer.capitaliserPhrases(texteNettoye) : objectifsNettoyes.get(0);
        return new DecompositionResult(false, List.of(unique), "");
    }

    // Nettoie un segment et le transforme en phrase d'action claire
    private static String formaterSegmentEnAction(String segment) {
        if (segment == null) return "";

        String clean = segment.trim();

        // 1. Normalisation des verbes d'action
        for (Map.Entry<String, String> entry : VERBES_ACTION_NORMALISES.entrySet()) {
            if (Pattern.compile(entry.getKey()).matcher(clean).find()) {
                clean = clean.replaceFirst(entry.getKey(), entry.getValue());
                break;
            }
        }

        // 2. Nettoyage des préfixes parasites
        for (String regexPrefix : PREFIXES_PARASITES) {
            clean = clean.replaceAll(regexPrefix, "");
        }

        clean = clean.replaceAll(SUFFIXES_PARASITES, "");
        clean = clean.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");

        if (clean.isBlank()) {
            return "";
        }

        // Capitaliser la première lettre
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    // Détecte si le segment n'est que du bruit conversationnel
    private static boolean estBruitPur(String text) {
        String lower = text.toLowerCase().trim();
        return lower.matches("^(?:merci|stp|svp|salut|bonjour|voila|c est tout|rien de plus|merci beaucoup|ok|d accord)$");
    }
}
