package test.integration;

import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import test.framework.Assert;
import test.framework.Test;

public class EndToEndIntegrationTest {

    @Test
    public void testScenario1IngenierieLogicielle() {
        String input = "Crée une API REST sécurisée en Spring Boot avec authentification JWT et rôles utilisateurs";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.CONCEPTION_ARCHITECTURE, profile.classification().primaryType(), "Doit classifier en architecture");
        Assert.assertContainsElement(profile.detectedTechnologies(), "Java", "Doit détecter Java/Spring");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "# RÔLE & EXPERTISE", "Le super prompt doit comporter le rôle");
        Assert.assertContains(outputPrompt, "<regles_de_developpement_strictes>", "Doit comporter les règles de développement");
        Assert.assertContains(outputPrompt, "<format_de_sortie>", "Doit comporter le format de sortie");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }

    @Test
    public void testScenario2GastronomieRecette() {
        String input = "Donne-moi la recette de la tarte au citron meringuée étape par étape";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.PROTOCOLE_RECETTE, profile.classification().primaryType(), "Doit classifier en recette");
        Assert.assertTrue(profile.domainInfo().isDomainIdentified(), "Le domaine culinaire doit être détecté");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "Chef Étoilé", "Le persona Chef doit être injecté");
        Assert.assertContains(outputPrompt, "<directives_culinaires>", "Doit comporter les directives culinaires");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }

    @Test
    public void testScenario3VulgarisationFeynman() {
        String input = "Explique-moi la relativité générale avec des analogies simples";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.CONCEPT_VULGARISATION, profile.classification().primaryType(), "Doit classifier en concept/vulgarisation");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "Richard Feynman", "Le persona Feynman doit être injecté");
        Assert.assertContains(outputPrompt, "<directives_feynman>", "Doit comporter les directives Feynman");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }

    @Test
    public void testScenario4DiagnosticDebug() {
        String input = "Debug cette NullPointerException dans la méthode UserService.login()";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.DEPANNAGE_DIAGNOSTIC, profile.classification().primaryType(), "Doit classifier en dépannage");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "<directives_de_diagnostic>", "Doit comporter les directives de diagnostic");
        Assert.assertContains(outputPrompt, "Cause Racine", "Doit exiger l'analyse de cause racine");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }

    @Test
    public void testScenario5ApprentissageMusique() {
        String input = "Je veux apprendre la guitare et comprendre les accords puis faire des exercices pour debutant";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.APPRENTISSAGE_TUTORIEL, profile.classification().primaryType(), "Doit classifier en apprentissage");
        Assert.assertTrue(profile.domainInfo().isDomainIdentified(), "Domaine musique identifié");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "Musicien", "Persona Musicien");
        Assert.assertContains(outputPrompt, "<objectifs_specifiques>", "Sous-objectifs présents");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }

    @Test
    public void testScenario6ComparaisonDecisionDatabase() {
        String input = "Comparatif entre PostgreSQL et MongoDB : lequel choisir pour une application bancaire ?";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.COMPARAISON_DECISION, profile.classification().primaryType(), "Doit classifier en comparaison");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "<directives_decisionnelles>", "Directives décisionnelles");
        Assert.assertContains(outputPrompt, "<format_de_sortie>", "Format de sortie");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }

    @Test
    public void testScenario7TraductionTechnique() {
        String input = "Traduis cette documentation technique en anglais";
        PromptProfile profile = Lemmatizer.analyser(input);

        Assert.assertEquals(TypeOfPrompt.CREATION_REDACTION, profile.classification().primaryType(), "Doit classifier en création/traduction");

        String outputPrompt = MetaPromptEngine.genererPromptOptimise(profile);
        Assert.assertContains(outputPrompt, "Anglais (EN)", "Langue cible Anglais");
        Assert.assertContains(outputPrompt, "<contexte_traduction>", "Contexte traduction");
        Assert.assertFalse(outputPrompt.contains("{{"), "Zéro tag résiduel");
    }
}
