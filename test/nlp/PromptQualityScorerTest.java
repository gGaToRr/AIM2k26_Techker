package test.nlp;

import nlp.PromptQualityScorer;
import test.framework.Assert;

public class PromptQualityScorerTest {

    public void testScorePromptVagueVsPromptPrecis() {
        String promptVague = "Fais un truc";
        PromptQualityScorer.Diagnostic diagVague = PromptQualityScorer.evaluer(promptVague, false, false, false);

        String promptPrecis = "Rédige une fonction Java 21 avec Spring Boot pour valider un token JWT en respectant les principes SOLID et en gérant les exceptions.";
        PromptQualityScorer.Diagnostic diagPrecis = PromptQualityScorer.evaluer(promptPrecis, true, true, false);

        Assert.assertTrue(diagPrecis.scoreGlobal() > diagVague.scoreGlobal(), "Un prompt précis et riche en contraintes doit avoir un score supérieur à un prompt vague");
        Assert.assertInRange(0, 100, diagPrecis.scoreGlobal(), "Le score total doit être compris entre 0 et 100");
    }

    public void testDiagnosticsRecommandations() {
        String promptCourt = "Aide-moi";
        PromptQualityScorer.Diagnostic diag = PromptQualityScorer.evaluer(promptCourt, false, true, false);
        Assert.assertNotNull(diag.pistesAmelioration(), "La liste des pistes d'amélioration ne doit pas être null");
    }
}
