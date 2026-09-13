package test.gen;

import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

public class MetaPromptEngineTest {

    public void testGenerationPromptAvecDomaineEtPersona() {
        String rawPrompt = "Donne-moi la recette de la tarte aux pommes pas a pas";
        PromptProfile profile = Lemmatizer.analyser(rawPrompt);
        String optimized = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertNotNull(optimized, "Le prompt optimisé ne doit pas être null");
        Assert.assertContains(optimized, "# RÔLE & EXPERTISE", "Doit contenir la section de rôle");
        Assert.assertContainsIgnoreCase(optimized, "Chef", "Doit contenir le persona d'un Chef");
        Assert.assertContains(optimized, "<demande_culinaire>", "Doit contenir la balise de demande culinaire");
        Assert.assertContains(optimized, "<format_de_sortie>", "Doit contenir le format de sortie");
    }

    public void testGenerationPromptAvecSousObjectifs() {
        String rawPrompt = "Je veux concevoir une base de données relationnelle puis écrire les requêtes SQL et ensuite créer les index";
        PromptProfile profile = Lemmatizer.analyser(rawPrompt);
        String optimized = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertContains(optimized, "<objectifs_specifiques>", "Doit contenir les sous-objectifs décomposés");
        Assert.assertContains(optimized, "base de données relationnelle", "Doit mentionner le premier objectif");
    }

    public void testGenerationTraductionAvecLangueCible() {
        String rawPrompt = "Traduis cette documentation en anglais";
        PromptProfile profile = Lemmatizer.analyser(rawPrompt);
        String optimized = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertContains(optimized, "<contexte_traduction>", "Doit contenir le contexte de traduction");
        Assert.assertContains(optimized, "Anglais (EN)", "Doit spécifier la langue cible Anglais");
    }

    public void testGenerationPromptCourtAvecAutoContraintes() {
        String rawPrompt = "Explique les trous noirs";
        PromptProfile profile = Lemmatizer.analyser(rawPrompt);
        String optimized = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertContains(optimized, "<directives_feynman>", "Doit utiliser les directives Feynman");
        Assert.assertContains(optimized, "<format_de_sortie>", "Doit contenir un format de sortie structuré");
    }

    public void testAbsenceEchappementHTML() {
        String rawPrompt = "Crée une classe Java avec List<String> et Map<String, Object>";
        PromptProfile profile = Lemmatizer.analyser(rawPrompt);
        String optimized = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertFalse(optimized.contains("&lt;"), "Les chevrons ne doivent pas être échappés en &lt;");
        Assert.assertFalse(optimized.contains("&gt;"), "Les chevrons ne doivent pas être échappés en &gt;");
        Assert.assertFalse(optimized.contains("&amp;"), "Les & ne doivent pas être échappés en &amp;");
    }
}
