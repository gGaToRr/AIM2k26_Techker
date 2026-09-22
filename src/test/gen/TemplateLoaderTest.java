package test.gen;

import gen.TemplateLoader;
import nlp.TypeOfPrompt;
import test.framework.Assert;

public class TemplateLoaderTest {

    public void testChargementTemplateApprentissage() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.APPRENTISSAGE_TUTORIEL, "guide_debutant");
        Assert.assertNotNull(template, "Le template guide_debutant doit être chargé");
        Assert.assertContains(template, "# RÔLE & EXPERTISE", "Le template doit contenir le header de rôle");
        Assert.assertContains(template, "<directives_pedagogiques>", "Le template doit contenir les directives pédagogiques");
    }

    public void testChargementTemplateArchitecture() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.CONCEPTION_ARCHITECTURE, "code_generation");
        Assert.assertNotNull(template, "Le template code_generation doit être chargé");
        Assert.assertContains(template, "<regles_de_developpement_strictes>", "Le template doit contenir les règles de développement");
    }

    public void testChargementTemplateTroubleshooting() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.DEPANNAGE_DIAGNOSTIC, "root_cause_debug");
        Assert.assertNotNull(template, "Le template root_cause_debug doit être chargé");
        Assert.assertContains(template, "<directives_de_diagnostic>", "Le template doit contenir les directives de diagnostic");
    }

    public void testChargementTemplateCreation() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.CREATION_REDACTION, "storytelling");
        Assert.assertNotNull(template, "Le template storytelling doit être chargé");
        Assert.assertContains(template, "<regles_narratives>", "Le template doit contenir les règles narratives");
    }

    public void testChargementTemplateProtocol() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.PROTOCOLE_RECETTE, "recette_culinaire");
        Assert.assertNotNull(template, "Le template recette_culinaire doit être chargé");
        Assert.assertContains(template, "<directives_culinaires>", "Le template doit contenir les directives culinaires");
    }

    public void testChargementTemplateComparison() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.COMPARAISON_DECISION, "matrice_comparative");
        Assert.assertNotNull(template, "Le template matrice_comparative doit être chargé");
        Assert.assertContains(template, "<directives_analyse>", "Le template doit contenir les directives d'analyse");
    }

    public void testChargementTemplateConcept() {
        String template = TemplateLoader.chargerTemplate(TypeOfPrompt.CONCEPT_VULGARISATION, "vulgarisation_feynman");
        Assert.assertNotNull(template, "Le template vulgarisation_feynman doit être chargé");
        Assert.assertContains(template, "<directives_feynman>", "Le template doit contenir les directives Feynman");
    }

    public void testFallbackTemplateSiInexistant() {
        String fallback = TemplateLoader.chargerTemplate(TypeOfPrompt.CONCEPT_VULGARISATION, "sous_type_inexistant_xyz");
        Assert.assertNotNull(fallback, "Le template de fallback ne doit pas être null");
        Assert.assertContains(fallback, "# RÔLE & EXPERTISE", "Le fallback doit contenir un rôle");
        Assert.assertContains(fallback, "<instruction_principale>", "Le fallback doit contenir l'instruction principale");
    }

    // --- Confinement des chemins de templates (Issue #58) ---

    public void testTemplateHorsPerimetreEstRejete() throws Exception {
        // Fichier .md bien reel, place hors des racines de templates autorisees
        java.nio.file.Path secret = java.nio.file.Path.of("secret_hors_perimetre.md");
        java.nio.file.Files.writeString(secret, "CONTENU CONFIDENTIEL");
        try {
            String[] tentatives = {
                    "../secret_hors_perimetre",
                    "../../secret_hors_perimetre",
                    "learning/../../secret_hors_perimetre",
                    "./../secret_hors_perimetre.md"
            };
            for (String tentative : tentatives) {
                Assert.assertTrue(TemplateLoader.chargerTemplateParNom(tentative).isEmpty(),
                        "Doit rejeter la remontee de repertoire : " + tentative);
                Assert.assertTrue(TemplateLoader.resoudreCheminTemplate(tentative + ".md").isEmpty(),
                        "Resolution refusee hors perimetre : " + tentative);
            }
        } finally {
            java.nio.file.Files.deleteIfExists(secret);
        }
    }

    public void testCheminAbsoluEstRejete() {
        Assert.assertTrue(TemplateLoader.resoudreCheminTemplate("/etc/passwd").isEmpty(),
                "Un chemin absolu ne doit jamais etre resolu");
        Assert.assertTrue(TemplateLoader.chargerTemplateParNom("/etc/hosts").isEmpty(),
                "Un chemin absolu ne doit jamais etre charge");
    }

    public void testResolutionNominaleInchangee() {
        // Un nom de template legitime reste resolu normalement
        Assert.assertTrue(TemplateLoader.resoudreCheminTemplate("learning/guide_debutant.md").isPresent(),
                "Le chemin relatif legitime doit rester resolu");
        Assert.assertTrue(TemplateLoader.chargerTemplateParNom("feynman").isPresent(),
                "La recherche par nom simple doit continuer a fonctionner");
    }

    public void testResolutionRefuseEntreeVideOuNulle() {
        Assert.assertTrue(TemplateLoader.resoudreCheminTemplate(null).isEmpty(), "Entree nulle refusee");
        Assert.assertTrue(TemplateLoader.resoudreCheminTemplate("   ").isEmpty(), "Entree vide refusee");
        Assert.assertTrue(TemplateLoader.resoudreDossierTemplate("../").isEmpty(), "Dossier hors perimetre refuse");
    }

    public void testResolutionDossierArchetypeAutorise() {
        Assert.assertTrue(TemplateLoader.resoudreDossierTemplate("learning").isPresent(),
                "Le dossier d'archetype legitime doit etre resolu");
    }
}
