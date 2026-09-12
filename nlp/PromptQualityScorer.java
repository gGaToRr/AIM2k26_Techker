package nlp;

import java.util.ArrayList;
import java.util.List;

// Évalue la qualité et la clarté du prompt sur 100
public class PromptQualityScorer {

    public record Diagnostic(
            int scoreGlobal,
            int scoreClarte,
            int scoreContexte,
            int scoreContraintes,
            List<String> pointsForts,
            List<String> pistesAmelioration
    ) {}

    // Analyse la clarté, le contexte et les contraintes données au LLM
    public static Diagnostic evaluer(String rawPrompt, boolean hasCode, boolean isCommand, boolean isQuestion) {
        if (rawPrompt == null || rawPrompt.isBlank()) {
            return new Diagnostic(0, 0, 0, 0, List.of(), List.of("Le prompt est vide."));
        }

        String lower = rawPrompt.toLowerCase();
        int words = rawPrompt.split("\\s+").length;

        int scoreClarte = 0;
        int scoreContexte = 0;
        int scoreContraintes = 0;

        List<String> pointsForts = new ArrayList<>();
        List<String> pistesAmelioration = new ArrayList<>();

        // 1. Clarté de la consigne
        if (isCommand || isQuestion) {
            scoreClarte += 25;
            pointsForts.add("Intention principale explicite.");
        } else {
            pistesAmelioration.add("Ajoutez un verbe d'action au début (ex: 'Écris', 'Analyse').");
        }

        if (words >= 8 && words <= 200) {
            scoreClarte += 15;
        } else if (words < 8) {
            scoreClarte += 5;
            pistesAmelioration.add("Prompt court : apportez plus de détails.");
        } else {
            scoreClarte += 10;
        }

        // 2. Contexte fourni
        if (hasCode) {
            scoreContexte += 25;
            pointsForts.add("Code source fourni.");
        } else if (words > 25) {
            scoreContexte += 20;
            pointsForts.add("Contexte textuel détaillé.");
        } else {
            scoreContexte += 10;
            pistesAmelioration.add("Précisez le contexte d'utilisation.");
        }

        // 3. Contraintes et format de sortie
        boolean hasFormatConstraint = lower.contains("format") || lower.contains("json") || lower.contains("markdown")
                || lower.contains("liste") || lower.contains("tableau") || lower.contains("court");

        boolean hasNegativeConstraint = lower.contains("ne pas") || lower.contains("sans") || lower.contains("evite")
                || lower.contains("uniquement");

        if (hasFormatConstraint) {
            scoreContraintes += 15;
            pointsForts.add("Format de réponse spécifié.");
        } else {
            pistesAmelioration.add("Spécifiez le format attendu (ex: 'en Markdown', 'en 3 points').");
        }

        if (hasNegativeConstraint) {
            scoreContraintes += 15;
            pointsForts.add("Contraintes négatives définies.");
        } else {
            pistesAmelioration.add("Ajoutez des contraintes à respecter (ex: 'sans dépendance externe').");
        }

        int scoreGlobal = Math.min(100, scoreClarte + scoreContexte + scoreContraintes);

        return new Diagnostic(scoreGlobal, scoreClarte, scoreContexte, scoreContraintes, pointsForts, pistesAmelioration);
    }
}
