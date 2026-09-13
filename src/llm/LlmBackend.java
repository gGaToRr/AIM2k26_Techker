package llm;

// Interface pour les moteurs d'inférence de modèles de langage locaux
public interface LlmBackend {

    @FunctionalInterface
    interface TokenConsumer {
        void accept(String token);
    }

    record GenerationResult(
            String fullText,
            int totalTokens,
            long elapsedMs,
            double tokensPerSecond,
            ModelType modelUsed
    ) {}

    boolean isAvailable(ModelType model, String modelsDir);

    GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer tokenConsumer) throws Exception;
}
