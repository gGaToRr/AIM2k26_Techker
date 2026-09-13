package nlp;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PromptProfile(
        String rawText,
        String sanitizedText,
        List<String> tokens,
        Map<String, Integer> lemmaFrequencies,
        Map<String, Double> weightedScores,
        double codeDensity,
        boolean isQuestion,
        boolean isCommand,
        String language,
        List<String> detectedTechnologies,
        Optional<String> targetTranslationLanguage,
        DomainExtractor.DomainInfo domainInfo,
        TokenCounter.TokenMetrics tokenMetrics,
        PromptQualityScorer.Diagnostic qualityDiagnostic,
        PromptClassifier.ClassificationResult classification
) {
    public boolean hasCode() {
        return codeDensity >= 0.15;
    }
}
