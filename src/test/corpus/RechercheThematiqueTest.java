package test.corpus;

import corpus.ConstruireIndex;
import corpus.Corpus;
import corpus.RechercheThematique;
import test.framework.AfterEach;
import test.framework.Assert;
import test.framework.BeforeEach;
import test.framework.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Recherche en deux temps : themes proches (seuls leurs fichiers sont lus), puis mots
public class RechercheThematiqueTest {

    private Path dossier;

    @BeforeEach
    public void preparer() throws Exception {
        dossier = Files.createTempDirectory("index_themes");
        List<Corpus.Prompt> prompts = List.of(
                new Corpus.Prompt("c1", "test", "fr", "Quelle recette de lasagnes végétariennes pour 6 personnes ?", 0.8),
                new Corpus.Prompt("c2", "test", "fr", "Recette de gâteau au chocolat sans oeufs", null),
                new Corpus.Prompt("a1", "test", "fr", "Mon chat refuse ses croquettes, que faire ?", null),
                new Corpus.Prompt("p1", "test", "fr", "Mon script python plante avec une erreur pandas sur un csv", null));
        Map<String, Map<String, List<ConstruireIndex.Entree>>> rangement = new TreeMap<>();
        for (Corpus.Prompt p : prompts) {
            ConstruireIndex.Entree e = new ConstruireIndex.Entree(p, List.of(p.id().startsWith("c") ? "cuisine"
                    : p.id().startsWith("a") ? "animaux" : "programmation"), "PROTOCOLE_RECETTE", 60);
            for (String theme : e.themes()) {
                rangement.computeIfAbsent("fr", l -> new TreeMap<>()).computeIfAbsent(theme, t -> new ArrayList<>()).add(e);
            }
        }
        ecrire(rangement);
    }

    private void ecrire(Map<String, Map<String, List<ConstruireIndex.Entree>>> rangement) throws Exception {
        ConstruireIndex.ecrire(dossier, rangement);
    }

    @AfterEach
    public void nettoyer() throws Exception {
        try (var flux = Files.walk(dossier)) {
            flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }
    }

    @Test
    public void testSeulsLesThemesProchesSontLus() {
        RechercheThematique.Resultat r = new RechercheThematique(dossier).rechercher(
                "donne moi une recette de lasagnes végétariennes", 3);

        Assert.assertEquals(List.of("cuisine"), r.themes(), "Theme cuisine");
        Assert.assertEquals(2, r.prompts(), "Seuls les 2 prompts du theme cuisine sont examines");
        Assert.assertEquals("c1", r.meilleurs().get(0).prompt().id(), "Les lasagnes d'abord");
    }

    @Test
    public void testIndexRelu() throws Exception {
        Assert.assertTrue(Files.exists(dossier.resolve("sommaire.json")), "Sommaire ecrit");
        String sommaire = Files.readString(dossier.resolve("sommaire.json"));
        Assert.assertContains(sommaire, "\"cuisine\":{\"libelle\":\"Cuisine et recettes\",\"prompts\":2", "Theme et nombre");
    }
}
