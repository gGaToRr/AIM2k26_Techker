package nlp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Compte et estime les tokens BPE pour les LLMs
public class TokenCounter {

    // Regex pour séparer en sous-mots / tokens BPE
    private static final Pattern BPE_TOKEN_PATTERN = Pattern.compile(
            "(?i)'[a-z]+|[\\p{L}]+|[\\p{N}]+|[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+"
    );

    // Conteneur des métriques de tokens
    public record TokenMetrics(
            int estimatedTokens,
            int characterCount,
            int wordCount,
            double estimatedCostDollars,
            String contextWindowFitness
    ) {}

    // Calcule le nombre de tokens et le coût estimé
    public static TokenMetrics analyser(String text) {
        if (text == null || text.isEmpty()) {
            return new TokenMetrics(0, 0, 0, 0.0, "0%");
        }

        int charCount = text.length();
        int wordCount = text.split("\\s+").length;

        Matcher matcher = BPE_TOKEN_PATTERN.matcher(text);
        int subwordTokens = 0;

        while (matcher.find()) {
            String token = matcher.group();
            // Les mots longs sont découpés en plusieurs tokens par les LLMs
            if (token.length() > 6) {
                subwordTokens += (int) Math.ceil(token.length() / 4.0);
            } else {
                subwordTokens++;
            }
        }

        // Prix moyen LLM (~2.50$ pour 1 million de tokens)
        double coutEstime = (subwordTokens / 1_000_000.0) * 2.50;
        double pourcentageContexte = (subwordTokens / 128_000.0) * 100.0;
        String fitness = String.format("%.2f%% (contexte 128K)", pourcentageContexte);

        return new TokenMetrics(subwordTokens, charCount, wordCount, coutEstime, fitness);
    }
}
