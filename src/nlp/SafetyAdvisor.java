package nlp;

import java.util.*;
import java.util.regex.Pattern;

// Détecte les termes susceptibles de déclencher les filtres de sécurité des LLMs (OpenAI, Anthropic, Google)
public class SafetyAdvisor {

    // Liste de mots et thématiques à haut risque de déclenchement de filtres
    private static final Map<String, String> TERMES_SENSIBLES = Map.ofEntries(
            // Armes & Explosifs
            Map.entry("bombe", "Matières dangereuses / Explosifs"),
            Map.entry("explosif", "Matières dangereuses / Explosifs"),
            Map.entry("detonateur", "Matières dangereuses / Explosifs"),
            Map.entry("tnt", "Matières dangereuses / Explosifs"),
            Map.entry("arme a feu", "Armes"),

            // Cyberattaques & Malwares
            Map.entry("ransomware", "Malware / Cyberattaque"),
            Map.entry("keylogger", "Malware / Cyberattaque"),
            Map.entry("trojan", "Malware / Cyberattaque"),
            Map.entry("ddos", "Cyberattaque"),
            Map.entry("exploit zero day", "Cyberattaque"),

            // Tentatives de Jailbreak
            Map.entry("ignore previous instructions", "Tentative de Jailbreak"),
            Map.entry("ignore toute instruction precedente", "Tentative de Jailbreak"),
            Map.entry("dan mode", "Tentative de Jailbreak"),
            Map.entry("jailbreak", "Tentative de Jailbreak")
    );

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

        for (Map.Entry<String, String> entry : TERMES_SENSIBLES.entrySet()) {
            String motCle = entry.getKey();
            Pattern p = Pattern.compile("(?i)(?<![a-zA-Z0-9])" + Pattern.quote(motCle) + "(?![a-zA-Z0-9])");
            if (p.matcher(lower).find()) {
                detectes.add(motCle);
                categories.add(entry.getValue());
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
