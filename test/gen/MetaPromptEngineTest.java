package test.gen;

import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

public class MetaPromptEngineTest {

    public void testResolutionCompleteVariablesMustacheSansResidu() {
        String[] testPrompts = {
                "Crée une API REST en Spring Boot avec Java 21",
                "Donne-moi la recette de la tarte au citron pas a pas",
                "Debug cette NullPointerException dans mon code",
                "Traduis cette documentation en anglais",
                "Explique les trous noirs avec la méthode Feynman",
                "Compare MongoDB et PostgreSQL pour un projet web",
                "Rédige une histoire de science-fiction sur Mars"
        };

        for (String rawPrompt : testPrompts) {
            PromptProfile profile = Lemmatizer.analyser(rawPrompt);
            String output = MetaPromptEngine.genererPromptOptimise(profile);

            Assert.assertNotNull(output, "Prompt généré non null pour: " + rawPrompt);
            Assert.assertFalse(output.isBlank(), "Prompt généré non vide");

            // Vérification stricte : aucun tag Mustache non résolu ne doit subsister
            Assert.assertFalse(output.contains("{{"), "Aucune balise Mustache résiduelle '{{' dans le rendu de: " + rawPrompt);
            Assert.assertFalse(output.contains("}}"), "Aucune balise Mustache résiduelle '}}' dans le rendu de: " + rawPrompt);

            // Vérification de la structure minimale obligatoire
            Assert.assertContains(output, "# RÔLE & EXPERTISE", "Section rôle présente");
            Assert.assertContains(output, "<format_de_sortie>", "Section format de sortie présente");
        }
    }

    public void testInjectionDynamiquePersonaDomaineEtSousObjectifs() {
        String prompt = "Je veux apprendre la guitare puis faire des exercices de solfège";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        String output = MetaPromptEngine.genererPromptOptimise(profile);

        // Persona Musique
        Assert.assertContainsIgnoreCase(output, "Musicien", "Persona Musicien injecté");

        // Objectifs décomposés
        Assert.assertContains(output, "<objectifs_specifiques>", "Balise sous-objectifs présente");
        Assert.assertContains(output, "guitare", "Objectif 1 mentionné");
        Assert.assertContains(output, "exercices", "Objectif 2 mentionné");
    }

    public void testPreservationStrictesCaracteresSpeciauxEtCode() {
        String codePrompt = "Écris une interface TypeScript: interface User<T> { id: number; data: T & { isValid: boolean }; }";
        PromptProfile profile = Lemmatizer.analyser(codePrompt);
        String output = MetaPromptEngine.genererPromptOptimise(profile);

        // Les chevrons génériques et ampersand ne doivent pas être convertis en entités HTML
        Assert.assertContains(output, "User<T>", "Génériques TypeScript intacts");
        Assert.assertContains(output, "T &", "Ampersand TypeScript intact");
        Assert.assertFalse(output.contains("&lt;"), "Pas de conversion HTML &lt;");
        Assert.assertFalse(output.contains("&gt;"), "Pas de conversion HTML &gt;");
        Assert.assertFalse(output.contains("&amp;"), "Pas de conversion HTML &amp;");
    }

    public void testTraductionAvecLangueCibleExplicite() {
        String prompt = "Traduis cette page d'aide en espagnol";
        PromptProfile profile = Lemmatizer.analyser(prompt);
        String output = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertContains(output, "Espagnol (ES)", "Langue cible Espagnol spécifiée");
        Assert.assertContains(output, "<contexte_traduction>", "Balise contexte traduction présente");
    }
}
