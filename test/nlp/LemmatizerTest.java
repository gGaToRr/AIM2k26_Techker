package test.nlp;

import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

import java.util.List;
import java.util.Map;

public class LemmatizerTest {

    public void testTokenisationEtStopwords() {
        String input = "Je veux créer une application en Java";
        List<String> tokens = Lemmatizer.tokeniser(input);
        Assert.assertContainsElement(tokens, "application", "La liste des tokens doit contenir 'application'");
        Assert.assertContainsElement(tokens, "java", "La liste des tokens doit contenir 'java'");
        Assert.assertFalse(tokens.contains("je"), "Les stopwords comme 'je' doivent être filtrés");
        Assert.assertFalse(tokens.contains("une"), "Les stopwords comme 'une' doivent être filtrés");
    }

    public void testExpressionsMultiMots() {
        String input = "Explique-moi pas a pas comment resoudre ce probleme";
        Map<String, Integer> freq = Lemmatizer.compterFrequences(input);
        Assert.assertTrue(freq.containsKey("pas a pas") || freq.containsKey("expliqu"), "L'expression multi-mots ou le lemme doit être comptabilisé");
    }

    public void testComptageFrequencesEtLemmes() {
        String input = "programmer des programmes en programmant";
        Map<String, Integer> freq = Lemmatizer.compterFrequences(input);
        Assert.assertTrue(freq.containsKey("programm"), "Les formes verbales doivent être réduites au lemme commun 'programm'");
    }

    public void testDensiteDeCode() {
        String codeSnippet = "public static void main(String[] args) { System.out.println(42); }";
        double densite = Lemmatizer.calculerDensiteCode(codeSnippet);
        Assert.assertGreaterThanOrEqual(0.15, densite, "La densité de code doit être significativement élevée pour du code Java");

        String texteNaturel = "C'est une belle journée pour apprendre la guitare.";
        double densiteTexte = Lemmatizer.calculerDensiteCode(texteNaturel);
        Assert.assertTrue(densiteTexte < 0.1, "La densité de code doit être très faible pour du texte naturel");
    }

    public void testDetectionLangueFrancaisVsAnglais() {
        String promptFr = "Comment fonctionne la mémoire vive dans un ordinateur ?";
        List<String> tokensFr = Lemmatizer.tokeniser(promptFr);
        Assert.assertEquals("FR", Lemmatizer.detecterLangue(promptFr, tokensFr), "Le français doit être détecté");

        String promptEn = "How does random access memory work in modern computers?";
        List<String> tokensEn = Lemmatizer.tokeniser(promptEn);
        Assert.assertEquals("EN", Lemmatizer.detecterLangue(promptEn, tokensEn), "L'anglais doit être détecté");
    }

    public void testAnalyseCompletePromptProfile() {
        String prompt = "Explique-moi la gravitation universelle";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertNotNull(profile, "Le profile ne doit pas être null");
        Assert.assertEquals("FR", profile.language(), "La langue du profile doit être FR");
        Assert.assertNotNull(profile.classification(), "La classification doit être présente");
    }
}
