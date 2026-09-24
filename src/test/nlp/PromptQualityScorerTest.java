package test.nlp;

import nlp.PromptQualityScorer;
import test.framework.Assert;
import test.framework.Test;

public class PromptQualityScorerTest {

    @Test
    public void testInvariantsDesScoresEtIntervalles() {
        String prompt = "Écris une fonction Java avec gestion des erreurs et tests";
        PromptQualityScorer.Diagnostic diag = PromptQualityScorer.evaluer(prompt, true, true, false);

        Assert.assertBetweenInclusive(0, 100, diag.scoreGlobal(), "Score global entre 0 et 100");
        Assert.assertBetweenInclusive(0, 40, diag.scoreClarte(), "Score clarté dans les bornes");
        Assert.assertBetweenInclusive(0, 35, diag.scoreContexte(), "Score contexte dans les bornes");
        Assert.assertBetweenInclusive(0, 25, diag.scoreContraintes(), "Score contraintes dans les bornes");
        Assert.assertFalse(diag.pointsForts().isEmpty(), "Au moins 1 point fort");
    }

    @Test
    public void testImpactCodeEtContraintesSurLeScore() {
        String promptBasique = "Fais un script";
        PromptQualityScorer.Diagnostic diagBasique = PromptQualityScorer.evaluer(promptBasique, false, false, false);

        String promptRiche = "Génère un composant React avec Tailwind en respectant la charte graphique et sans utiliser de dépendances externes";
        PromptQualityScorer.Diagnostic diagRiche = PromptQualityScorer.evaluer(promptRiche, true, true, false);

        Assert.assertTrue(diagRiche.scoreGlobal() > diagBasique.scoreGlobal(), "Le prompt riche doit avoir un score strictement supérieur");
        Assert.assertTrue(diagRiche.scoreContexte() >= diagBasique.scoreContexte(), "Score contexte supérieur");
    }

    @Test
    public void testPromptVideScoreNul() {
        PromptQualityScorer.Diagnostic vide = PromptQualityScorer.evaluer("", false, false, false);
        Assert.assertEquals(0, vide.scoreGlobal(), "Score nul pour prompt vide");
        Assert.assertEquals(0, vide.scoreClarte(), "Score clarté nul");
        Assert.assertEquals(0, vide.scoreContexte(), "Score contexte nul");
        Assert.assertEquals(0, vide.scoreContraintes(), "Score contraintes nul");
        Assert.assertFalse(vide.pistesAmelioration().isEmpty(), "Pistes d'amélioration fournies");

        PromptQualityScorer.Diagnostic nul = PromptQualityScorer.evaluer(null, false, false, false);
        Assert.assertEquals(0, nul.scoreGlobal(), "Score nul pour null");
    }
}
