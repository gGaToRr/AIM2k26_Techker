package nlp;

import java.util.*;

// Détermine l'Archétype Universel du prompt avec une distribution de confiance probabiliste
public class PromptClassifier {

    public enum ConfidenceLevel {
        HIGH, MEDIUM, LOW
    }

    public record ClassificationResult(
            TypeOfPrompt primaryType,
            double primaryProbability,
            ConfidenceLevel confidenceLevel,
            Map<TypeOfPrompt, Double> distribution,
            String justification
    ) {}

    // Calcule la répartition des probabilités sur chacun des 7 Archétypes Universels
    public static ClassificationResult classifier(PromptProfile profile, List<String> techStack, Optional<String> langueCible) {
        Map<TypeOfPrompt, Double> scores = new EnumMap<>(TypeOfPrompt.class);
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            scores.put(type, 0.1);
        }

        Map<String, Double> weights = profile.weightedScores();

        // 1. APPRENTISSAGE_TUTORIEL
        double learningScore = weights.getOrDefault("apprend", 0.0) * 4.0;
        scores.put(TypeOfPrompt.APPRENTISSAGE_TUTORIEL, scores.get(TypeOfPrompt.APPRENTISSAGE_TUTORIEL) + learningScore);

        // 2. CONCEPTION_ARCHITECTURE
        double archScore = weights.getOrDefault("architect", 0.0) * 3.5 + weights.getOrDefault("code", 0.0) * 2.5;
        if (profile.hasCode()) {
            archScore += profile.codeDensity() * 12.0;
        }
        if (!techStack.isEmpty()) {
            archScore += techStack.size() * 6.0;
        }
        scores.put(TypeOfPrompt.CONCEPTION_ARCHITECTURE, scores.get(TypeOfPrompt.CONCEPTION_ARCHITECTURE) + archScore);

        // 3. DEPANNAGE_DIAGNOSTIC
        double troubleScore = weights.getOrDefault("depann", 0.0) * 4.0;
        scores.put(TypeOfPrompt.DEPANNAGE_DIAGNOSTIC, scores.get(TypeOfPrompt.DEPANNAGE_DIAGNOSTIC) + troubleScore);

        // 4. CREATION_REDACTION
        double createScore = weights.getOrDefault("creer", 0.0) * 3.5 + weights.getOrDefault("corrig", 0.0) * 3.5 + weights.getOrDefault("traduir", 0.0) * 3.5;
        if (langueCible.isPresent()) {
            createScore += 10.0;
        }
        scores.put(TypeOfPrompt.CREATION_REDACTION, scores.get(TypeOfPrompt.CREATION_REDACTION) + createScore);

        // 5. PROTOCOLE_RECETTE
        double protocolScore = weights.getOrDefault("protocol", 0.0) * 4.5;
        scores.put(TypeOfPrompt.PROTOCOLE_RECETTE, scores.get(TypeOfPrompt.PROTOCOLE_RECETTE) + protocolScore);

        // 6. COMPARAISON_DECISION
        double comparisonScore = weights.getOrDefault("compar", 0.0) * 4.5;
        scores.put(TypeOfPrompt.COMPARAISON_DECISION, scores.get(TypeOfPrompt.COMPARAISON_DECISION) + comparisonScore);

        // 7. CONCEPT_VULGARISATION
        double conceptScore = weights.getOrDefault("concept", 0.0) * 3.5 + weights.getOrDefault("expliqu", 0.0) * 2.5 + weights.getOrDefault("question", 0.0) * 2.0;
        if (profile.isQuestion()) {
            conceptScore += 2.0;
        }

        // Fallback encyclopédique si aucune intention d'action spécifique n'est détectée
        boolean aucuneIntentionSpecifique = (learningScore == 0.0 && archScore == 0.0 && troubleScore == 0.0 && createScore == 0.0 && protocolScore == 0.0 && comparisonScore == 0.0 && conceptScore == 0.0);
        if (aucuneIntentionSpecifique) {
            conceptScore += 2.0;
        }
        scores.put(TypeOfPrompt.CONCEPT_VULGARISATION, scores.get(TypeOfPrompt.CONCEPT_VULGARISATION) + conceptScore);

        // Normalisation en pourcentages
        double totalScore = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<TypeOfPrompt, Double> distribution = new EnumMap<>(TypeOfPrompt.class);

        TypeOfPrompt topType = TypeOfPrompt.CONCEPT_VULGARISATION;
        double maxProb = 0.0;

        for (Map.Entry<TypeOfPrompt, Double> entry : scores.entrySet()) {
            double prob = totalScore > 0 ? (entry.getValue() / totalScore) * 100.0 : 0.0;
            distribution.put(entry.getKey(), Math.round(prob * 10.0) / 10.0);
            if (prob > maxProb) {
                maxProb = prob;
                topType = entry.getKey();
            }
        }

        ConfidenceLevel level = maxProb >= 50.0 ? ConfidenceLevel.HIGH : (maxProb >= 30.0 ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW);

        String justification = switch (topType) {
            case APPRENTISSAGE_TUTORIEL -> "Intention d'apprentissage, d'initiation ou guide pédagogique progressif";
            case CONCEPTION_ARCHITECTURE -> "Conception de système, code source, design UI/UX ou arborescence de fichiers (" + (techStack.isEmpty() ? "modélisation" : String.join(", ", techStack)) + ")";
            case DEPANNAGE_DIAGNOSTIC -> "Diagnostic de panne, résolution de bugs, audit de sécurité ou refactoring";
            case CREATION_REDACTION -> "Génération narrative, écriture créative, réécriture, relecture ou traduction";
            case PROTOCOLE_RECETTE -> "Protocole opératoire, recette culinaire, procédure chronologique ou checklist";
            case COMPARAISON_DECISION -> "Analyse comparative multicritères, benchmark ou arbitrage décisionnel";
            case CONCEPT_VULGARISATION -> aucuneIntentionSpecifique
                    ? "Sujet ou terme isolé : synthèse encyclopédique et explicative"
                    : "Explication de concept théorique, définition ou vulgarisation scientifique";
        };

        return new ClassificationResult(topType, maxProb, level, distribution, justification);
    }
}
