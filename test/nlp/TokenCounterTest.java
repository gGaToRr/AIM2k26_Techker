package test.nlp;

import nlp.TokenCounter;
import test.framework.Assert;

public class TokenCounterTest {

    public void testEstimationTokensPromptCourt() {
        String prompt = "Bonjour monde";
        TokenCounter.TokenMetrics metrics = TokenCounter.analyser(prompt);
        Assert.assertNotNull(metrics, "Les métriques ne doivent pas être null");
        Assert.assertTrue(metrics.estimatedTokens() > 0, "Le nombre de tokens estimés doit être > 0");
    }

    public void testCalculCoutsAPI() {
        String prompt = "Un texte plus long contenant plusieurs phrases pour évaluer le coût approximatif en entrée.";
        TokenCounter.TokenMetrics metrics = TokenCounter.analyser(prompt);
        Assert.assertTrue(metrics.estimatedCostDollars() >= 0.0, "Le coût estimé doit être >= 0");
        Assert.assertNotNull(metrics.contextWindowFitness(), "Le contextWindowFitness doit être renseigné");
    }

    public void testEntreeVide() {
        TokenCounter.TokenMetrics metrics = TokenCounter.analyser("");
        Assert.assertEquals(0, metrics.estimatedTokens(), "Une chaîne vide doit avoir 0 tokens");
    }
}
