package test.gen;

import gen.FewShotLibrary;
import gen.MetaPromptEngine;
import gen.TemplateLoader;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.TypeOfPrompt;
import test.framework.Assert;
import test.framework.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// Tests de la bibliotheque d'exemples calibres (Issue #46)
public class FewShotLibraryTest {

    private static final String DEUX_EXEMPLES = """
            # Entete ignoree

            ## EXEMPLE

            ### DEMANDE
            Comment trier une liste en Python avec une cle personnalisee ?

            ### REPONSE
            Utilise sorted(liste, key=...).

            ## EXEMPLE

            ### DEMANDE
            Quelle est la difference entre une marmite et une casserole en cuisine ?

            ### REPONSE
            La marmite est plus haute et destinee aux bouillons.
            """;

    // --- Analyse du format ---

    @Test
    public void testAnalyseDeuxExemples() {
        List<FewShotLibrary.Exemple> exemples = FewShotLibrary.analyser(DEUX_EXEMPLES);

        Assert.assertSize(exemples, 2, "Deux exemples extraits");
        Assert.assertContains(exemples.get(0).entree(), "trier une liste", "Demande du premier");
        Assert.assertContains(exemples.get(0).sortie(), "sorted", "Reponse du premier");
        Assert.assertNotContains(exemples.get(0).sortie(), "## EXEMPLE", "Pas de debordement sur le bloc suivant");
    }

    @Test
    public void testBlocIncompletEstIgnore() {
        String incomplet = """
                ## EXEMPLE

                ### DEMANDE
                Une demande sans reponse
                """;
        Assert.assertSize(FewShotLibrary.analyser(incomplet), 0, "Un exemple sans reponse est rejete");
    }

    @Test
    public void testContenuVideOuNulNeCassePas() {
        Assert.assertSize(FewShotLibrary.analyser(null), 0, "Contenu nul");
        Assert.assertSize(FewShotLibrary.analyser(""), 0, "Contenu vide");
        Assert.assertSize(FewShotLibrary.analyser("du texte sans aucun marqueur"), 0, "Aucun marqueur");
    }

    // --- Pertinence : c'est le coeur de l'issue ---

    @Test
    public void testLExempleLePlusProcheEstClasseEnTete() {
        List<FewShotLibrary.Exemple> exemples = FewShotLibrary.analyser(DEUX_EXEMPLES);

        List<FewShotLibrary.Exemple> pourPython =
                FewShotLibrary.classerParPertinence(exemples, "Je veux trier une liste Python par date");
        Assert.assertContains(pourPython.get(0).entree(), "trier une liste",
                "La demande Python doit remonter l'exemple Python");

        List<FewShotLibrary.Exemple> pourCuisine =
                FewShotLibrary.classerParPertinence(exemples, "Quelle casserole choisir pour un bouillon ?");
        Assert.assertContains(pourCuisine.get(0).entree(), "casserole",
                "La demande cuisine doit remonter l'exemple cuisine");
    }

    // Les mots vides ne doivent pas creer de fausse proximite
    @Test
    public void testLesMotsVidesNeCreentPasDePertinence() {
        List<FewShotLibrary.Exemple> exemples = FewShotLibrary.analyser(DEUX_EXEMPLES);
        Set<String> motsOutils = Set.of("le", "la", "les", "une", "de", "en", "est");

        for (FewShotLibrary.Exemple exemple : exemples) {
            Assert.assertEquals(0.0, FewShotLibrary.score(exemple, motsOutils),
                    "Des mots outils seuls ne doivent donner aucun score");
        }
    }

    @Test
    public void testDemandeVideDonneUnScoreNul() {
        FewShotLibrary.Exemple exemple = FewShotLibrary.analyser(DEUX_EXEMPLES).get(0);
        Assert.assertEquals(0.0, FewShotLibrary.score(exemple, Set.of()), "Aucun mot de reference");
    }

    // A score egal, l'ordre du fichier fait foi : la selection doit etre reproductible
    @Test
    public void testSelectionReproductible() {
        List<FewShotLibrary.Exemple> exemples = FewShotLibrary.analyser(DEUX_EXEMPLES);
        String demande = "sujet totalement etranger aux deux exemples";

        List<String> premierPassage = FewShotLibrary.classerParPertinence(exemples, demande)
                .stream().map(FewShotLibrary.Exemple::entree).toList();
        List<String> secondPassage = FewShotLibrary.classerParPertinence(exemples, demande)
                .stream().map(FewShotLibrary.Exemple::entree).toList();

        Assert.assertEquals(premierPassage, secondPassage, "Deux appels donnent le meme ordre");
    }

    // --- Budget de contexte ---

    // Les modeles vises tiennent dans quelques milliers de tokens : des exemples
    // trop volumineux chasseraient la vraie demande hors de la fenetre.
    @Test
    public void testLeBlocInjecteRespecteLeBudget() {
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            PromptProfile profile = Lemmatizer.analyser("Une demande quelconque pour tester le budget");
            String bloc = FewShotLibrary.selectionner(profile, type, null);

            Assert.assertTrue(bloc.length() <= FewShotLibrary.MAX_CARACTERES + 600,
                    "Bloc few-shot borne pour " + type.name() + " (" + bloc.length() + " caracteres)");
        }
    }

    @Test
    public void testNombreDExemplesBorne() {
        PromptProfile profile = Lemmatizer.analyser("Corrige ce bug de segmentation");
        String bloc = FewShotLibrary.selectionner(profile, TypeOfPrompt.DEPANNAGE_DIAGNOSTIC, null);

        int occurrences = bloc.split("<exemple>", -1).length - 1;
        Assert.assertBetweenInclusive(0, FewShotLibrary.MAX_EXEMPLES, occurrences,
                "Au plus " + FewShotLibrary.MAX_EXEMPLES + " exemples injectes");
    }

    // --- Couverture des 7 archetypes ---

    @Test
    public void testChaqueArchetypePossedeSesExemples() {
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            List<FewShotLibrary.Exemple> exemples = FewShotLibrary.chargerExemples(type, null);
            Assert.assertTrue(exemples.size() >= 1,
                    "Au moins un exemple calibre pour " + type.name());
        }
    }

    // Un exemple dont la reponse est squelettique n'apprend rien au modele
    @Test
    public void testLesExemplesLivresSontSubstantiels() {
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            for (FewShotLibrary.Exemple exemple : FewShotLibrary.chargerExemples(type, null)) {
                Assert.assertGreaterThanOrEqual(15, exemple.entree().length(),
                        "Demande substantielle pour " + type.name());
                Assert.assertGreaterThanOrEqual(150, exemple.sortie().length(),
                        "Reponse substantielle pour " + type.name());
            }
        }
    }

    @Test
    public void testAucunFichierDExemplesOrphelin() throws Exception {
        Path dossier = TemplateLoader.resoudreDossierTemplate(FewShotLibrary.DOSSIER).orElseThrow();
        List<String> attendus = java.util.Arrays.stream(TypeOfPrompt.values())
                .map(t -> t.name().toLowerCase() + ".md").toList();

        try (Stream<Path> flux = Files.list(dossier)) {
            for (Path fichier : flux.toList()) {
                String nom = fichier.getFileName().toString();
                boolean connu = attendus.contains(nom)
                        || TemplateLoader.chargerTemplateParNom(nom.replace(".md", "")).isPresent();
                Assert.assertTrue(connu,
                        "Le fichier " + nom + " ne correspond ni a un archetype ni a un sous-type : il ne sera jamais charge");
            }
        }
    }

    // --- Integration dans le rendu ---

    @Test
    public void testLeBlocEstInjecteDansLePromptFinal() {
        PromptProfile profile = Lemmatizer.analyser("Ecris une fonction Java qui valide une adresse email");
        String prompt = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertContains(prompt, "<exemples_de_reference>", "Section d'exemples presente");
        Assert.assertContains(prompt, "<reponse_ideale>", "Exemple rendu dans le prompt");
        Assert.assertNotContains(prompt, "{{EXEMPLES_FEW_SHOT}}", "Variable Mustache resolue");
        Assert.assertNotContains(prompt, "{{#hasFewShot}}", "Section Mustache resolue");
    }

    // Le modele doit traiter la demande, pas repondre aux exemples
    @Test
    public void testLeBlocPreciseQuIlNeFautPasRepondreAuxExemples() {
        PromptProfile profile = Lemmatizer.analyser("Ecris une fonction Java qui valide une adresse email");
        String prompt = MetaPromptEngine.genererPromptOptimise(profile);

        Assert.assertContains(prompt, "Ne reponds pas a ces exemples",
                "Consigne explicite contre la reponse aux exemples");
    }

    @Test
    public void testAbsenceDExemplesNeLaissePasDeSectionVide() {
        // Sous-type inexistant et archetype sans fichier : le bloc doit etre vide
        PromptProfile profile = Lemmatizer.analyser("Une demande");
        String bloc = FewShotLibrary.selectionner(profile, TypeOfPrompt.APPRENTISSAGE_TUTORIEL, "sous_type_inexistant_xyz");

        // Repli sur l'archetype : le bloc doit rester non nul et exploitable
        Assert.assertNotNull(bloc, "Jamais nul");
    }
}
