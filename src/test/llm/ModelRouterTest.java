package test.llm;

import llm.ModelRouter;
import llm.ModelType;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;
import test.framework.Test;

// Tests unitaires pour le routeur intelligent ModelRouter
public class ModelRouterTest {

    @Test
    public void testRoutingOnCodeAndArchitecture() {
        // Prompt orienté code Java
        PromptProfile profileCode = Lemmatizer.analyser("Comment implémenter un arbre binaire de recherche en Java avec la méthode insert ? public class Node {}");
        ModelRouter.RoutingDecision decision = ModelRouter.route(profileCode);
        Assert.assertEquals(ModelType.QWEN_CODER, decision.selectedModel(), "Un prompt avec du code doit être routé vers Qwen Coder");
        Assert.assertTrue(decision.confidenceScore() >= 0.85, "Score de confiance élevé pour le code");
        Assert.assertContains(decision.rationale(), "Qwen", "La justification doit mentionner Qwen");
    }

    @Test
    public void testRoutingOnDebugging() {
        PromptProfile profileDebug = Lemmatizer.analyser("J'ai une NullPointerException à la ligne 42 dans mon contrôleur Spring Boot, trouve le bug");
        ModelRouter.RoutingDecision decision = ModelRouter.route(profileDebug);
        Assert.assertEquals(ModelType.QWEN_CODER, decision.selectedModel(), "Un prompt de débogage doit être routé vers Qwen Coder");
    }

    @Test
    public void testRoutingOnReasoningAndComparison() {
        PromptProfile profileComparison = Lemmatizer.analyser("Compare PostgreSQL et MongoDB pour une application bancaire : analyse les avantages, inconvénients et fais une recommandation justifiée");
        ModelRouter.RoutingDecision decision = ModelRouter.route(profileComparison);
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, decision.selectedModel(), "Un comparatif complexe doit être routé vers DeepSeek R1");
        Assert.assertContains(decision.rationale(), "DeepSeek", "La justification doit mentionner DeepSeek");
    }

    @Test
    public void testRoutingOnLearningAndGeneral() {
        PromptProfile profileLearning = Lemmatizer.analyser("Explique-moi l'histoire de la Renaissance en France comme si j'avais 10 ans");
        ModelRouter.RoutingDecision decision = ModelRouter.route(profileLearning);
        Assert.assertEquals(ModelType.GEMMA_GENERAL, decision.selectedModel(), "Un prompt d'apprentissage/culture doit être routé vers Gemma 2");
    }

    @Test
    public void testRoutingOnShortFastQuestion() {
        PromptProfile profileShort = Lemmatizer.analyser("Quelle est la capitale de l'Australie ?");
        ModelRouter.RoutingDecision decision = ModelRouter.route(profileShort);
        Assert.assertEquals(ModelType.SMOLLM_FAST, decision.selectedModel(), "Une question directe courte doit être routée vers SmolLM2");
    }

    @Test
    public void testManualOverrideFlag() {
        PromptProfile profileGeneral = Lemmatizer.analyser("Rédige un poème sur la mer");

        // Force Qwen via override
        ModelRouter.RoutingDecision overrideQwen = ModelRouter.resolve("qwen", profileGeneral);
        Assert.assertEquals(ModelType.QWEN_CODER, overrideQwen.selectedModel(), "L'override doit forcer Qwen");
        Assert.assertTrue(overrideQwen.isManualOverride(), "Doit être marqué comme override manuel");

        // Force DeepSeek via override
        ModelRouter.RoutingDecision overrideR1 = ModelRouter.resolve("r1", profileGeneral);
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, overrideR1.selectedModel(), "L'override doit forcer DeepSeek");

        // Auto resolution
        ModelRouter.RoutingDecision autoDecision = ModelRouter.resolve("auto", profileGeneral);
        Assert.assertFalse(autoDecision.isManualOverride(), "Auto ne doit pas être un override forcé");
    }

    @Test
    public void testNullProfileFallback() {
        ModelRouter.RoutingDecision decision = ModelRouter.route(null);
        Assert.assertEquals(ModelType.GEMMA_GENERAL, decision.selectedModel(), "Fallback sur Gemma en cas de profil null");
    }
}
