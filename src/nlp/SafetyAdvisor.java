package nlp;

import java.util.*;
import java.util.regex.Pattern;

// Détecte les termes susceptibles de déclencher les filtres de sécurité des LLMs (OpenAI, Anthropic, Google)
public class SafetyAdvisor {

    // Règle de détection : terme sensible, sa catégorie et son pattern précompilé
    private record TermRule(String keyword, String category, Pattern pattern) {}

    // Liste de mots et thématiques à haut risque de déclenchement de filtres.
    // LinkedHashMap (et non Map.ofEntries) pour garantir un ordre d'itération stable : Map.ofEntries
    // randomise son ordre à chaque lancement de JVM, ce qui rendait l'ordre des termes cités dans le
    // message d'avertissement imprévisible d'une exécution à l'autre pour un même prompt.
    private static final Map<String, String> TERMES_SENSIBLES = new LinkedHashMap<>();

    // Règles précompilées, construites une seule fois au chargement de la classe
    // (évite de recompiler chaque regex à chaque appel de analyser)
    private static final List<TermRule> REGLES;

    static {
        // Armes & Explosifs
        TERMES_SENSIBLES.put("bombe", "Matières dangereuses / Explosifs");
        TERMES_SENSIBLES.put("explosif", "Matières dangereuses / Explosifs");
        TERMES_SENSIBLES.put("detonateur", "Matières dangereuses / Explosifs");
        TERMES_SENSIBLES.put("tnt", "Matières dangereuses / Explosifs");
        TERMES_SENSIBLES.put("arme a feu", "Armes");

        // Cyberattaques & Malwares
        TERMES_SENSIBLES.put("ransomware", "Malware / Cyberattaque");
        TERMES_SENSIBLES.put("keylogger", "Malware / Cyberattaque");
        TERMES_SENSIBLES.put("trojan", "Malware / Cyberattaque");
        TERMES_SENSIBLES.put("ddos", "Cyberattaque");
        TERMES_SENSIBLES.put("exploit zero day", "Cyberattaque");

        // Tentatives de Jailbreak
        TERMES_SENSIBLES.put("ignore previous instructions", "Tentative de Jailbreak");
        TERMES_SENSIBLES.put("ignore toute instruction precedente", "Tentative de Jailbreak");
        TERMES_SENSIBLES.put("dan mode", "Tentative de Jailbreak");
        TERMES_SENSIBLES.put("jailbreak", "Tentative de Jailbreak");

        List<TermRule> regles = new ArrayList<>(TERMES_SENSIBLES.size());
        for (Map.Entry<String, String> entry : TERMES_SENSIBLES.entrySet()) {
            Pattern p = Pattern.compile("(?i)(?<![a-zA-Z0-9])" + Pattern.quote(entry.getKey()) + "(?![a-zA-Z0-9])");
            regles.add(new TermRule(entry.getKey(), entry.getValue(), p));
        }
        REGLES = Collections.unmodifiableList(regles);
    }

    public record SafetyReport(
            boolean containsSensitiveTerms,
            List<String> detectedTerms,
            List<String> categories,
            String warningMessage
    ) {}

    // Analyse le texte et génère un avertissement pédagogique sans bloquer
    public static SafetyReport analyser(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new SafetyReport(false, List.of(), List.of(), "");
        }

        String lower = " " + rawText.toLowerCase().replace("'", " ") + " ";
        List<String> detectes = new ArrayList<>();
        Set<String> categories = new LinkedHashSet<>();

        for (TermRule regle : REGLES) {
            if (regle.pattern().matcher(lower).find()) {
                detectes.add(regle.keyword());
                categories.add(regle.category());
            }
        }

        if (detectes.isEmpty()) {
            return new SafetyReport(false, List.of(), List.of(), "");
        }

        String warning = "⚠️ Attention : ce prompt contient des termes sensibles (" + String.join(", ", detectes) +
                ") susceptibles d'être bloqués par les filtres des LLMs (OpenAI, Anthropic, Gemini). Retirez-les ou reformulez pour un meilleur résultat.";

        return new SafetyReport(true, detectes, new ArrayList<>(categories), warning);
    }
}
