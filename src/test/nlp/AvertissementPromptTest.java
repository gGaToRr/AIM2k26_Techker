package test.nlp;

import nlp.AvertissementPrompt;
import test.framework.Assert;
import test.framework.Test;

// Avertissement "prompt trop court" : moins de 100 caracteres utiles
public class AvertissementPromptTest {

    @Test
    public void testPromptCourtAverti() {
        Assert.assertEquals("Attention votre prompt initial contient trop peu d'information.",
                AvertissementPrompt.verifier("fais un site web").orElse(null), "Message exact");
    }

    @Test
    public void testSeuilDeCentCaracteres() {
        Assert.assertTrue(AvertissementPrompt.verifier("a".repeat(99)).isPresent(), "99 caracteres : averti");
        Assert.assertTrue(AvertissementPrompt.verifier("a".repeat(100)).isEmpty(), "100 caracteres : pas d'avertissement");
    }

    // Des espaces autour ne rendent pas un prompt plus informatif
    @Test
    public void testEspacesIgnores() {
        Assert.assertTrue(AvertissementPrompt.verifier("   " + "a".repeat(98) + "   \n").isPresent(), "Espaces exclus");
    }

    @Test
    public void testPromptVideOuNul() {
        Assert.assertTrue(AvertissementPrompt.verifier(null).isPresent(), "Nul");
        Assert.assertTrue(AvertissementPrompt.verifier("").isPresent(), "Vide");
    }
}
