package nlp;

import java.util.*;

// Détermine la nature du prompt avec un pourcentage de confiance
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

    // Calcule la répartition des probabilités sur chaque type de prompt
    public static ClassificationResult classifier(PromptProfile profile, List<String> techStack, Optional<String> langueCible) {
        Map<TypeOfPrompt, Double> scores = new EnumMap<>(TypeOfPrompt.class);
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            scores.put(type, 0.1);
        }

        Map<String, Double> weights = profile.weightedScores();

        // 1. Scoring CODE
        double codeScore = weights.getOrDefault("code", 0.0) * 1.5 + weights.getOrDefault("debug", 0.0) * 1.8;
        if (profile.hasCode()) {
            codeScore += profile.codeDensity() * 15.0;
        }
        if (!techStack.isEmpty()) {
            codeScore += techStack.size() * 3.5;
        }
        scores.put(TypeOfPrompt.CODE, scores.get(TypeOfPrompt.CODE) + codeScore);

        // 2. Scoring TRANSLATE
        double translateScore = weights.getOrDefault("traduir", 0.0) * 3.0;
        if (langueCible.isPresent()) {
            translateScore += 8.0;
        }
        scores.put(TypeOfPrompt.TRANSLATE, scores.get(TypeOfPrompt.TRANSLATE) + translateScore);

        // 3. Scoring CORRECTANSWERS
        double correctScore = weights.getOrDefault("corrig", 0.0) * 3.0;
        scores.put(TypeOfPrompt.CORRECTANSWERS, scores.get(TypeOfPrompt.CORRECTANSWERS) + correctScore);

        // 4. Scoring CREATION
        double createScore = weights.getOrDefault("creer", 0.0) * 2.5;
        scores.put(TypeOfPrompt.CREATION, scores.get(TypeOfPrompt.CREATION) + createScore);

        // 5. Scoring FACTUALQUESTIONS
        double questionScore = weights.getOrDefault("expliqu", 0.0) * 2.0 + weights.getOrDefault("question", 0.0) * 2.0;
        if (profile.isQuestion()) {
            questionScore += 2.5;
        }
        scores.put(TypeOfPrompt.FACTUALQUESTIONS, scores.get(TypeOfPrompt.FACTUALQUESTIONS) + questionScore);

        // Normalisation en pourcentages
        double totalScore = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<TypeOfPrompt, Double> distribution = new EnumMap<>(TypeOfPrompt.class);

        TypeOfPrompt topType = TypeOfPrompt.FACTUALQUESTIONS;
        double maxProb = 0.0;

        for (Map.Entry<TypeOfPrompt, Double> entry : scores.entrySet()) {
            double prob = totalScore > 0 ? (entry.getValue() / totalScore) * 100.0 : 0.0;
            distribution.put(entry.getKey(), Math.round(prob * 10.0) / 10.0);
            if (prob > maxProb) {
                maxProb = prob;
                topType = entry.getKey();
            }
        }

        ConfidenceLevel level = maxProb >= 55.0 ? ConfidenceLevel.HIGH : (maxProb >= 35.0 ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW);

        String justification = switch (topType) {
            case CODE -> "Code ou technologies détectés (" + techStack + ")";
            case TRANSLATE -> "Demande de traduction" + (langueCible.map(l -> " vers " + l).orElse(""));
            case CORRECTANSWERS -> "Mots-clés de correction / relecture dominants";
            case CREATION -> "Génération créative ou rédaction narrative";
            case FACTUALQUESTIONS -> "Question factuelle ou demande d'explication";
        };

        return new ClassificationResult(topType, maxProb, level, distribution, justification);
    }
}
