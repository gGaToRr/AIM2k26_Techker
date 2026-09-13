package test.integration;

import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import test.framework.Assert;

public class EndToEndIntegrationTest {

    public void testScenarioIngenierieLogicielle() {
        String input = "Crée une API REST sécurisée en Spring Boot avec authentification JWT et rôles utilisateurs";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.CONCEPTION_ARCHITECTURE, profile.classification().primaryType(), "Doit classifier en architecture");
        Assert.assertContainsElement(profile.detectedTechnologies(), "Java", "Doit détecter Java/Spring");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "# RÔLE & EXPERTISE", "Le super prompt doit comporter le rôle");
        Assert.assertContains(outputPrompt, "<regles_de_developpement_strictes>", "Doit comporter les règles de développement");
        Assert.assertContains(outputPrompt, "<format_de_sortie>", "Doit comporter le format de sortie");
    }

    public void testScenarioGastronomieRecette() {
        String input = "Donne-moi la recette de la tarte au citron meringuée étape par étape";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.PROTOCOLE_RECETTE, profile.classification().primaryType(), "Doit classifier en recette");
        Assert.assertTrue(profile.domainInfo().isDomainIdentified(), "Le domaine culinaire doit être détecté");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "Chef Étoilé", "Le persona Chef doit être injecté");
        Assert.assertContains(outputPrompt, "<directives_culinaires>", "Doit comporter les directives culinaires");
    }

    public void testScenarioVulgarisationFeynman() {
        String input = "Explique-moi la relativité générale avec des analogies simples";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.CONCEPT_VULGARISATION, profile.classification().primaryType(), "Doit classifier en concept/vulgarisation");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "Richard Feynman", "Le persona Feynman doit être injecté");
        Assert.assertContains(outputPrompt, "<directives_feynman>", "Doit comporter les directives Feynman");
    }

    public void testScenarioDiagnosticDebug() {
        String input = "Debug cette NullPointerException dans la méthode UserService.login()";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.DEPANNAGE_DIAGNOSTIC, profile.classification().primaryType(), "Doit classifier en dépannage");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "<directives_de_diagnostic>", "Doit comporter les directives de diagnostic");
        Assert.assertContains(outputPrompt, "Cause Racine", "Doit exiger l'analyse de cause racine");
    }
}
