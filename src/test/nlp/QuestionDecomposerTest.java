package test.nlp;

import nlp.QuestionDecomposer;
import test.framework.Assert;

public class QuestionDecomposerTest {

    public void testDecompositionFluxAvecConnecteurPuis() {
        String prompt = "Je veux créer une API puis la sécuriser avec un token JWT";
        QuestionDecomposer.DecompositionResult result = QuestionDecomposer.decomposer(prompt);
        Assert.assertTrue(result.hasMultipleObjectives(), "Le prompt doit être décomposé en sous-objectifs");
        Assert.assertEquals(2, result.objectives().size(), "Il doit y avoir exactement 2 sous-objectifs");
        Assert.assertContains(result.xmlFormattedList(), "<objectifs_specifiques>", "La sortie XML doit être structurée");
    }

    public void testDecompositionFluxAvecConnecteurEnsuiteEtAussi() {
        String prompt = "Fais la pâte à pizza ensuite prépare la sauce tomate et aussi ajoute les champignons";
        QuestionDecomposer.DecompositionResult result = QuestionDecomposer.decomposer(prompt);
        Assert.assertTrue(result.hasMultipleObjectives(), "Le prompt multi-actions doit être décomposé");
        Assert.assertTrue(result.objectives().size() >= 2, "Il doit y avoir au moins 2 sous-objectifs");
    }

    public void testPromptSimpleSansDecomposition() {
        String prompt = "Donne-moi la capitale de la France";
        QuestionDecomposer.DecompositionResult result = QuestionDecomposer.decomposer(prompt);
        Assert.assertFalse(result.hasMultipleObjectives(), "Un prompt direct simple ne doit pas être sur-découpé");
        Assert.assertEquals(1, result.objectives().size(), "Un prompt simple compte 1 seul objectif");
    }

    public void testEntreeVide() {
        QuestionDecomposer.DecompositionResult result = QuestionDecomposer.decomposer("");
        Assert.assertFalse(result.hasMultipleObjectives(), "Une chaîne vide n'a pas d'objectifs multiples");
    }
}
