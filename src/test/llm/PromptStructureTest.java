package test.llm;

import llm.PromptStructure;
import llm.PromptStructure.Section;
import test.framework.Assert;
import test.framework.Test;

import java.util.List;
import java.util.Map;

// Mise au meme format du prompt ameliore, quelle que soit la presentation du modele.
// Les reponses ci-dessous reprennent les formes reellement produites par les modeles.
public class PromptStructureTest {

    private static final String ATTENDU = """
            ## Rôle
            Tu es un expert en développement web.

            ## Contexte
            Un site pour mon restaurant.

            ## Tâche
            Concevoir le site avec le menu et les réservations.

            ## Contraintes
            - Responsive
            - Sécurisé

            ## Format de sortie attendu
            Un plan de projet.""";

    // Qwen : titres en texte brut, contenu sur la meme ligne
    @Test
    public void testTitresEnTexteBrut() {
        String reponse = """
                Role: Tu es un expert en développement web.
                Contexte: Un site pour mon restaurant.
                Tache: Concevoir le site avec le menu et les réservations.
                Contraintes:
                - Responsive
                - Sécurisé
                Format de sortie attendu: Un plan de projet.""";
        Assert.assertEquals(ATTENDU, PromptStructure.structurer(reponse, "fr").orElse(null), "Format commun");
    }

    // Gemma : preambule, titres en gras, puces en etoile
    @Test
    public void testTitresEnGrasEtPreambule() {
        String reponse = """
                ## Prompt amélioré :

                **Rôle :** Tu es un expert en développement web.
                **Contexte :** Un site pour mon restaurant.
                **Tâche :**  Concevoir le site avec le menu et les réservations.
                **Contraintes :**
                * Responsive
                * Sécurisé
                **Format de sortie attendu :** Un plan de projet.""";
        Assert.assertEquals(ATTENDU, PromptStructure.structurer(reponse, "fr").orElse(null), "Format commun");
    }

    // Titres Markdown, et variantes anglaises
    @Test
    public void testTitresMarkdownEtAnglais() {
        String reponse = """
                ### Role
                Tu es un expert en développement web.
                ### Context
                Un site pour mon restaurant.
                ### Task
                Concevoir le site avec le menu et les réservations.
                ### Constraints
                - Responsive
                - Sécurisé
                ### Output format
                Un plan de projet.""";
        Assert.assertEquals(ATTENDU, PromptStructure.structurer(reponse, "fr").orElse(null), "Format commun");
    }

    // "- Format : PDF" est une contrainte, pas le titre de la section Format
    @Test
    public void testPuceNestPasUnTitre() {
        Map<Section, String> sections = PromptStructure.extraire("""
                Contraintes :
                - Format : PDF
                - Contexte : bureau""");
        Assert.assertEquals("- Format : PDF\n- Contexte : bureau", sections.get(Section.CONTRAINTES), "Contraintes intactes");
        Assert.assertFalse(sections.containsKey(Section.FORMAT), "Pas de section Format");
    }

    // Mots-cles en vrac : pas un prompt structure
    @Test
    public void testReponseNonStructureeRefusee() {
        Assert.assertTrue(PromptStructure.structurer("Site web restaurant, menu et reservations, HTML, CSS.", "fr")
                .isEmpty(), "Refusee");
        Assert.assertTrue(PromptStructure.structurer("Rôle : expert\nTâche : trier", "fr").isEmpty(),
                "Deux sections ne suffisent pas");
    }

    // Section absente : visible et a completer, la forme reste la meme
    @Test
    public void testSectionAbsenteACompleter() {
        String rendu = PromptStructure.structurer("Rôle : Tu es un expert.\nContexte : X.\nTâche : Y.", "fr").get();
        Assert.assertContains(rendu, "## Contraintes\n[à compléter]", "Contraintes a completer");
        Assert.assertContains(rendu, "## Format de sortie attendu\n[à compléter]", "Format a completer");
    }

    @Test
    public void testTitresDansLaLangueDuPrompt() {
        String reponse = "Rôle : Tu es un expert.\nContexte : X.\nTâche : Y.";
        Assert.assertContains(PromptStructure.structurer(reponse, "EN").get(), "## Expected output format\n[to be completed]",
                "Anglais");
        Assert.assertContains(PromptStructure.structurer(reponse, "es").get(), "## Tarea\nY.", "Espagnol");
    }

    // DeepSeek deroule toute la solution dans le role ; les modeles repetent des contraintes
    @Test
    public void testRoleEnUnePhraseEtContraintesSansDoublon() {
        Map<Section, String> sections = PromptStructure.extraire("""
                Rôle : Tu es un expert web. Tu devrais concevoir un site. Tu pourrais utiliser HTML.
                Contraintes :
                - Accessible
                - Sécurisé
                -  Accessible""");
        Assert.assertEquals("Tu es un expert web.", sections.get(Section.ROLE), "Premiere phrase");
        Assert.assertEquals("- Accessible\n- Sécurisé", sections.get(Section.CONTRAINTES), "Doublon retire");
    }

    // L'exemple de la consigne (e-mail d'augmentation) recopie dans un autre sujet
    @Test
    public void testEmpruntsALExempleRetires() {
        String reponse = """
                Rôle : Tu es un expert web.
                Contexte :
                Je veux un site pour mon restaurant. Mon poste : [préciser le poste].
                Tâche : Concevoir le site.
                Contraintes :
                - Responsive
                - 150 mots maximum""";
        String rendu = PromptStructure.structurer(reponse, "fr", "un site pour mon restaurant").get();
        Assert.assertNotContains(rendu, "Mon poste", "Emprunt retire");
        Assert.assertNotContains(rendu, "150 mots", "Emprunt retire");
        Assert.assertContains(rendu, "- Responsive", "Contenu propre garde");

        String demande = PromptStructure.structurer(reponse, "fr", "un texte de 150 mots sur mon restaurant").get();
        Assert.assertContains(demande, "150 mots maximum", "Garde quand le prompt brut le demande");
    }

    @Test
    public void testResteSurLeSujet() {
        List<String> motsCles = List.of("script", "python", "fichier", "csv", "memoryerror", "pandas", "pc");
        Assert.assertTrue(PromptStructure.resteSurLeSujet("Lis le fichier CSV avec pandas par morceaux.", motsCles),
                "Meme sujet");
        Assert.assertFalse(PromptStructure.resteSurLeSujet("Crée un site web sur le jardinage.", motsCles), "Hors sujet");
        Assert.assertTrue(PromptStructure.resteSurLeSujet("Peu importe.", List.of("tri")), "Trop peu de mots-cles");
    }
}
