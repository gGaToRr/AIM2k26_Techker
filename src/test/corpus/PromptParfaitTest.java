package test.corpus;

import corpus.ConstruireIndex;
import corpus.Corpus;
import corpus.PromptParfait;
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

// Le prompt parfait : aiguillage par les themes, classement selon la richesse lexicale,
// au moins la moitie des mots precis de l'utilisateur, longueur utilisable par un petit modele
public class PromptParfaitTest {

    private Path dossier;

    private static ConstruireIndex.Entree entree(String id, String texte, String theme, int score) {
        return new ConstruireIndex.Entree(new Corpus.Prompt(id, "test", "fr", texte, null), List.of(theme),
                "PROTOCOLE_RECETTE", score);
    }

    @BeforeEach
    public void preparer() throws Exception {
        dossier = Files.createTempDirectory("base_prompts");
        List<ConstruireIndex.Entree> entrees = List.of(
                entree("lasagnes", "Donne-moi une recette de lasagnes végétariennes pour 6 personnes, avec une béchamel "
                        + "sans gluten, un temps de préparation de 45 minutes maximum et la liste des courses.", "cuisine", 70),
                entree("gateau", "Recette de gâteau au chocolat sans oeufs pour un anniversaire de 10 enfants", "cuisine", 60),
                entree("court", "lasagnes ?", "cuisine", 20),
                entree("long", "Recette de lasagnes végétariennes : " + "étape détaillée. ".repeat(120), "cuisine", 90));
        Map<String, Map<String, List<ConstruireIndex.Entree>>> rangement = new TreeMap<>();
        for (ConstruireIndex.Entree e : entrees) {
            rangement.computeIfAbsent("fr", l -> new TreeMap<>()).computeIfAbsent(e.themes().get(0), t -> new ArrayList<>()).add(e);
        }
        ConstruireIndex.ecrire(dossier, rangement);
    }

    @AfterEach
    public void nettoyer() throws Exception {
        try (var flux = Files.walk(dossier)) {
            flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }
    }

    @Test
    public void testPromptParfaitTrouve() {
        PromptParfait.Reference r = PromptParfait.trouverDans(dossier, "recette de lasagnes végétariennes pour ce soir")
                .orElseThrow();
        Assert.assertEquals("lasagnes", r.entree().prompt().id(), "Le prompt le plus proche et utilisable");
        Assert.assertEquals(List.of("cuisine"), r.themes(), "Aiguillage par le theme");
        Assert.assertTrue(Math.round(r.couverture() * r.richesse()) >= PromptParfait.motsCommunsExiges(r.richesse()),
                "Assez de mots communs avec l'utilisateur");
        Assert.assertTrue(r.richesse() >= 3, "Richesse lexicale mesuree");
    }

    // Trop court ou trop long (7 000 caracteres colles) : inutilisable comme reference
    @Test
    public void testLongueurUtilisable() {
        PromptParfait.Reference r = PromptParfait.trouverDans(dossier, "lasagnes végétariennes").orElseThrow();
        Assert.assertFalse(List.of("court", "long").contains(r.entree().prompt().id()), "Ni trop court ni trop long");
    }

    // Aucun prompt ne contient la moitie des mots de l'utilisateur : pas de reference
    @Test
    public void testPasDeReferenceTropEloignee() {
        Assert.assertTrue(PromptParfait.trouverDans(dossier, "recette de couscous royal marocain aux merguez")
                .isEmpty(), "Aucune reference");
    }

    // Mots communs exiges : la moitie des mots precis, 3 au plus
    @Test
    public void testMotsCommunsExigesSelonLaRichesse() {
        Assert.assertEquals(1, PromptParfait.motsCommunsExiges(2), "Prompt pauvre");
        Assert.assertEquals(2, PromptParfait.motsCommunsExiges(4), "Prompt moyen");
        Assert.assertEquals(3, PromptParfait.motsCommunsExiges(8), "Prompt riche : 3 suffisent");
    }

    // Prompt pauvre : le classement s'appuie davantage sur la qualite ; prompt riche : sur les mots
    @Test
    public void testPoidsSelonLaRichesseLexicale() {
        Assert.assertEquals(0.5, RechercheThematique.poidsDesMots(1), "Prompt pauvre");
        Assert.assertEquals(0.7, RechercheThematique.poidsDesMots(3), "Prompt moyen");
        Assert.assertEquals(0.85, RechercheThematique.poidsDesMots(10), "Prompt riche");
    }

    // Les tests desactivent la base de la machine : resultats reproductibles
    @Test
    public void testBaseDesactiveeDansLesTests() {
        Assert.assertFalse(PromptParfait.disponible(), "Base de la machine ignoree");
        Assert.assertTrue(PromptParfait.trouver("recette de lasagnes").isEmpty(), "Pas de reference");
    }
}
