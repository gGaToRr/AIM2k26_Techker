package llm;

import nlp.PromptProfile;
import nlp.TypeOfPrompt;

import java.util.Optional;

// Aiguille intelligemment chaque prompt vers le modèle léger le plus spécialisé
public class ModelRouter {

    public record RoutingDecision(
            ModelType selectedModel,
            String rationale,
            double confidenceScore,
            boolean isManualOverride
    ) {}

    // Sélectionne automatiquement le modèle optimal en fonction du profil sémantique NLP
    public static RoutingDecision route(PromptProfile profile) {
        if (profile == null) {
            return new RoutingDecision(
                    ModelType.GEMMA_GENERAL,
                    "Profil NLP non disponible : sélection par défaut du modèle généraliste Gemma 2.",
                    0.50,
                    false
            );
        }

        // 1. Détection prioritaire de code / programmation / architecture
        boolean hasSignificantCode = profile.codeDensity() >= 0.15;
        boolean hasTechStack = !profile.detectedTechnologies().isEmpty();
        TypeOfPrompt primaryType = profile.classification() != null ? profile.classification().primaryType() : TypeOfPrompt.CREATION_REDACTION;
        boolean isCodingArchetype = primaryType == TypeOfPrompt.CONCEPTION_ARCHITECTURE
                || primaryType == TypeOfPrompt.DEPANNAGE_DIAGNOSTIC;

        if (hasSignificantCode || (hasTechStack && isCodingArchetype)) {
            String details = hasSignificantCode ? "densité de code (" + (int)(profile.codeDensity() * 100) + "%)" : "stack technique détectée (" + String.join(", ", profile.detectedTechnologies()) + ")";
            return new RoutingDecision(
                    ModelType.QWEN_CODER,
                    "Tâche technique orientée code : " + details + " -> Qwen 2.5 Coder est le spécialiste optimal.",
                    0.95,
                    false
            );
        }

        if (isCodingArchetype) {
            return new RoutingDecision(
                    ModelType.QWEN_CODER,
                    "Archétype " + primaryType.getLabel() + " -> Qwen 2.5 Coder sélectionné pour la structure logicielle.",
                    0.90,
                    false
            );
        }

        // 2. Détection de raisonnement logique complexe / comparaison
        boolean isReasoningArchetype = primaryType == TypeOfPrompt.COMPARAISON_DECISION
                || primaryType == TypeOfPrompt.PROTOCOLE_RECETTE;
        boolean hasDecomposition = profile.decomposition() != null && profile.decomposition().hasMultipleObjectives();

        if (isReasoningArchetype || hasDecomposition) {
            String details = isReasoningArchetype ? "archétype " + primaryType.getLabel() : "démarche multi-objectifs";
            return new RoutingDecision(
                    ModelType.DEEPSEEK_REASONING,
                    "Nécessite un raisonnement logique arborescent (" + details + ") -> DeepSeek R1 sélectionné pour sa chaîne de pensée (CoT).",
                    0.92,
                    false
            );
        }

        // 3. Tâche très courte / question directe rapide sur prompt compact
        int wordCount = profile.tokenMetrics() != null ? profile.tokenMetrics().wordCount() : 0;
        if (wordCount > 0 && wordCount < 12 && profile.isQuestion() && !hasTechStack) {
            return new RoutingDecision(
                    ModelType.SMOLLM_FAST,
                    "Question courte et directe (" + wordCount + " mots) -> SmolLM2 sélectionné pour sa rapidité maximale.",
                    0.85,
                    false
            );
        }

        // 4. Rédaction, apprentissage, vulgarisation ou généraliste
        return new RoutingDecision(
                ModelType.GEMMA_GENERAL,
                "Archétype " + primaryType.getLabel() + " et rédaction en français -> Gemma 2 sélectionné pour son éloquence et sa clarté pédagogique.",
                0.88,
                false
        );
    }

    // Résout le modèle en tenant compte d'un éventuel override explicite utilisateur (--model)
    public static RoutingDecision resolve(String modelOverride, PromptProfile profile) {
        if (modelOverride != null && !modelOverride.isBlank() && !"auto".equalsIgnoreCase(modelOverride.trim())) {
            Optional<ModelType> forced = ModelType.fromAlias(modelOverride);
            if (forced.isPresent()) {
                return new RoutingDecision(
                        forced.get(),
                        "Sélection manuelle forcée par l'utilisateur via --model (" + modelOverride + ").",
                        1.0,
                        true
                );
            }
        }
        return route(profile);
    }
}
