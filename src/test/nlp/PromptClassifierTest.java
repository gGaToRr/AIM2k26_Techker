package test.nlp;

import nlp.Lemmatizer;
import nlp.PromptClassifier;
import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import test.framework.Assert;
import test.framework.Test;

public class PromptClassifierTest {

    @Test
    public void testClassificationApprentissageTutoriel() {
        String prompt = "Je veux apprendre la guitare et comprendre les accords de base avec un cours débutant";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.APPRENTISSAGE_TUTORIEL, profile.classification().primaryType(), "Doit être classé en APPRENTISSAGE_TUTORIEL");
    }

    @Test
    public void testClassificationConceptionArchitecture() {
        String prompt = "Crée une API REST sécurisée en Spring Boot avec architecture en couches";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.CONCEPTION_ARCHITECTURE, profile.classification().primaryType(), "Doit être classé en CONCEPTION_ARCHITECTURE");
    }

    @Test
    public void testClassificationDepannageDiagnostic() {
        String prompt = "Debug cette NullPointerException dans mon service d'authentification";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.DEPANNAGE_DIAGNOSTIC, profile.classification().primaryType(), "Doit être classé en DEPANNAGE_DIAGNOSTIC");
    }

    @Test
    public void testClassificationCreationRedaction() {
        String prompt = "Rédige une histoire de science-fiction se déroulant sur Mars en 2080";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.CREATION_REDACTION, profile.classification().primaryType(), "Doit être classé en CREATION_REDACTION");
    }

    @Test
    public void testClassificationProtocoleRecette() {
        String prompt = "Donne-moi la recette de la tarte au citron meringuée étape par étape";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.PROTOCOLE_RECETTE, profile.classification().primaryType(), "Doit être classé en PROTOCOLE_RECETTE");
    }

    @Test
    public void testClassificationComparaisonDecision() {
        String prompt = "Comparatif entre PostgreSQL et MongoDB : lequel choisir pour mon projet ?";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.COMPARAISON_DECISION, profile.classification().primaryType(), "Doit être classé en COMPARAISON_DECISION");
    }

    @Test
    public void testClassificationConceptVulgarisation() {
        String prompt = "Explique-moi la théorie de la relativité générale avec des mots simples";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        Assert.assertEquals(TypeOfPrompt.CONCEPT_VULGARISATION, profile.classification().primaryType(), "Doit être classé en CONCEPT_VULGARISATION");
    }

    @Test
    public void testProbabilitesSoftmaxEtNiveauDeConfiance() {
        String prompt = "Écris du code Java pour trier une liste";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        PromptClassifier.ClassificationResult result = profile.classification();
        Assert.assertTrue(result.primaryProbability() > 0.0, "La probabilité primaire doit être > 0");
        Assert.assertNotNull(result.confidenceLevel(), "Le niveau de confiance ne doit pas être null");
    }
}
