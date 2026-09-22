package test.nlp;

import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;
import test.framework.Test;

import java.util.List;
import java.util.Map;

public class LemmatizerTest {

    @Test
    public void testTokenisationEtFiltrageStopwordsExhaustif() {
        String input = "Je veux créer une belle application en Java pour les utilisateurs du web";
        List<String> tokens = Lemmatizer.tokeniser(input);

        // Mots utiles présents
        Assert.assertContainsElement(tokens, "creer", "Le verbe 'créer' (normalisé) doit être présent");
        Assert.assertContainsElement(tokens, "application", "'application' doit être présent");
        Assert.assertContainsElement(tokens, "java", "'java' doit être présent");
        Assert.assertContainsElement(tokens, "utilisateurs", "'utilisateurs' doit être présent");
        Assert.assertContainsElement(tokens, "web", "'web' doit être présent");

        // Stopwords strictement absents
        Assert.assertNotContainsElement(tokens, "je", "Stopword 'je' absent");
        Assert.assertNotContainsElement(tokens, "une", "Stopword 'une' absent");
        Assert.assertNotContainsElement(tokens, "en", "Stopword 'en' absent");
        Assert.assertNotContainsElement(tokens, "pour", "Stopword 'pour' absent");
        Assert.assertNotContainsElement(tokens, "les", "Stopword 'les' absent");
        Assert.assertNotContainsElement(tokens, "du", "Stopword 'du' absent");
    }

    @Test
    public void testDecoupageCamelCaseEtSymboles() {
        String input = "getUserProfileById parseXmlDocument";
        List<String> tokens = Lemmatizer.tokeniser(input);
        Assert.assertContainsElement(tokens, "get", "CamelCase 'get' extrait");
        Assert.assertContainsElement(tokens, "user", "CamelCase 'user' extrait");
        Assert.assertContainsElement(tokens, "profile", "CamelCase 'profile' extrait");
        Assert.assertContainsElement(tokens, "xml", "CamelCase 'xml' extrait");
        Assert.assertContainsElement(tokens, "document", "CamelCase 'document' extrait");
    }

    @Test
    public void testExpressionsMultiMotsPasseAPasEtRootCause() {
        String input = "Explique-moi pas a pas comment analyser la root cause de cette panne";
        Map<String, Integer> freq = Lemmatizer.compterFrequences(input);

        Assert.assertTrue(freq.containsKey("pas a pas") || freq.containsKey("expliqu"), "L'expression 'pas a pas' ou le lemme 'expliqu' doit être indexé");
        Assert.assertTrue(freq.containsKey("root cause") || freq.containsKey("depann") || freq.containsKey("caus"), "L'expression 'root cause' ou le lemme de panne doit être indexé");
    }

    @Test
    public void testRacinisationFormesVerbalesEtPluriels() {
        String input = "analysons analyseur analytique analyses";
        Map<String, Integer> freq = Lemmatizer.compterFrequences(input);
        Assert.assertTrue(freq.containsKey("analys"), "Les variantes de analyser doivent pointer vers le lemme 'analys'");
    }

    @Test
    public void testCalculDensiteDeCodeStrict() {
        String pureCode = "public static void main(String[] args) {\n    int x = 42;\n    return x;\n}";
        double densiteCode = Lemmatizer.calculerDensiteCode(pureCode);
        Assert.assertGreaterThanOrEqual(0.20, densiteCode, "Densité d'un code source structuré >= 0.20");

        String pureTexte = "Le soleil se lève à l'est et illumine les montagnes le matin.";
        double densiteTexte = Lemmatizer.calculerDensiteCode(pureTexte);
        Assert.assertInRange(0.0, 0.05, densiteTexte, "Densité d'un texte naturel purement narratif <= 0.05");
    }

    @Test
    public void testDetectionLangueStrictementEvaluee() {
        String promptFr = "Bonjour, pouvez-vous m'expliquer le théorème de Pythagore ?";
        Assert.assertEquals("FR", Lemmatizer.detecterLangue(promptFr, Lemmatizer.tokeniser(promptFr)), "Doit détecter FR");

        String promptEn = "Hello, can you please explain how binary search trees work in computer science?";
        Assert.assertEquals("EN", Lemmatizer.detecterLangue(promptEn, Lemmatizer.tokeniser(promptEn)), "Doit détecter EN");
    }

    @Test
    public void testDetectionCommandeEtQuestion() {
        String commande = "Crée une classe Java avec deux attributs";
        List<String> tokensCmd = Lemmatizer.tokeniser(commande);
        Assert.assertTrue(Lemmatizer.detecterCommande(tokensCmd), "Doit identifier un verbe d'action au mode impératif");

        String question = "Pourquoi le ciel est bleu ?";
        List<String> tokensQ = Lemmatizer.tokeniser(question);
        Assert.assertTrue(Lemmatizer.detecterQuestion(question, tokensQ), "Doit identifier une question ('Pourquoi' et '?')");
    }

    @Test
    public void testIntegriteProfileSurPromptComplexe() {
        String prompt = "Je veux créer une API en Spring Boot puis la tester avec JUnit";
        PromptProfile profile = Lemmatizer.analyser(prompt);

        Assert.assertNotNull(profile, "Profile non null");
        Assert.assertEquals(prompt, profile.rawText(), "Texte brut préservé");
        Assert.assertFalse(profile.sanitizedText().isEmpty(), "Texte assaini non vide");
        Assert.assertFalse(profile.tokens().isEmpty(), "Tokens présents");
        Assert.assertTrue(profile.isCommand(), "Est une commande");
        Assert.assertEquals("FR", profile.language(), "Langue FR");
        Assert.assertTrue(profile.decomposition().hasMultipleObjectives(), "Décomposition active");
        Assert.assertSize(profile.decomposition().objectives(), 2, "Exactement 2 objectifs");
        Assert.assertNotNull(profile.domainInfo(), "DomainInfo initialisé");
        Assert.assertNotNull(profile.tokenMetrics(), "TokenMetrics initialisé");
        Assert.assertNotNull(profile.qualityDiagnostic(), "Diagnostic initialisé");
        Assert.assertNotNull(profile.classification(), "Classification initialisée");
    }

    @Test
    public void testProfileSurEntreeVideOuNull() {
        PromptProfile emptyProfile = Lemmatizer.analyser("");
        Assert.assertNotNull(emptyProfile, "Profile sur vide non null");
        Assert.assertEquals(0, emptyProfile.tokenMetrics().estimatedTokens(), "0 tokens");

        PromptProfile nullProfile = Lemmatizer.analyser(null);
        Assert.assertNotNull(nullProfile, "Profile sur null non null");
    }
}
