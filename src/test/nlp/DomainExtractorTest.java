package test.nlp;

import nlp.DomainExtractor;
import test.framework.Assert;

public class DomainExtractorTest {

    public void testExtractionToutesLes10FamillesDeDomaines() {
        // 1. Musique
        DomainExtractor.DomainInfo musique = DomainExtractor.analyser("Comment accorder une guitare et jouer du piano ?");
        Assert.assertEquals("Musique & Lutherie", musique.domainName(), "Domaine Musique");
        Assert.assertTrue(musique.isDomainIdentified(), "Identifié");
        Assert.assertContains(musique.expertPersona(), "Musicien", "Persona Musicien");

        // 2. Gastronomie
        DomainExtractor.DomainInfo cuisine = DomainExtractor.analyser("Recette pour faire un gâteau au chocolat au four");
        Assert.assertEquals("Gastronomie & Arts Culinaires", cuisine.domainName(), "Domaine Gastronomie");
        Assert.assertContains(cuisine.expertPersona(), "Chef", "Persona Chef");

        // 3. Aéronautique
        DomainExtractor.DomainInfo aero = DomainExtractor.analyser("Comment un avion génère de la portance en vol ?");
        Assert.assertEquals("Aéronautique & Systèmes de Transport", aero.domainName(), "Domaine Aéronautique");
        Assert.assertContains(aero.expertPersona(), "Aéronautique", "Persona Aéro");

        // 4. Astrophysique
        DomainExtractor.DomainInfo astro = DomainExtractor.analyser("Explique la formation d'un trou noir dans une galaxie");
        Assert.assertEquals("Astrophysique & Sciences Fondamentales", astro.domainName(), "Domaine Astrophysique");
        Assert.assertContains(astro.expertPersona(), "Astrophysicien", "Persona Astrophysicien");

        // 5. Génie Mécanique
        DomainExtractor.DomainInfo meca = DomainExtractor.analyser("Fonctionnement d'un moteur à piston hydraulique");
        Assert.assertEquals("Génie Mécanique & Industrie", meca.domainName(), "Domaine Mécanique");
        Assert.assertContains(meca.expertPersona(), "Mécanicien", "Persona Mécanicien");

        // 6. Sport & Biomécanique
        DomainExtractor.DomainInfo sport = DomainExtractor.analyser("Programme de musculation et cardio pour un marathon");
        Assert.assertEquals("Sciences du Sport & Biomécanique", sport.domainName(), "Domaine Sport");
        Assert.assertContains(sport.expertPersona(), "Athlètes", "Persona Préparateur Physique");

        // 7. Histoire & Civilisations
        DomainExtractor.DomainInfo histoire = DomainExtractor.analyser("Histoire de la Révolution française et de l'Empire");
        Assert.assertEquals("Histoire & Sciences des Civilisations", histoire.domainName(), "Domaine Histoire");
        Assert.assertContains(histoire.expertPersona(), "Historien", "Persona Historien");

        // 8. Droit & Réglementation
        DomainExtractor.DomainInfo droit = DomainExtractor.analyser("Règles relatives au contrat de travail et code civil");
        Assert.assertEquals("Droit & Réglementation", droit.domainName(), "Domaine Droit");
        Assert.assertContains(droit.expertPersona(), "Juriste", "Persona Juriste");

        // 9. Économie & Marchés Financiers
        DomainExtractor.DomainInfo finance = DomainExtractor.analyser("Impact de l'inflation sur les actions en bourse");
        Assert.assertEquals("Économie & Marchés Financiers", finance.domainName(), "Domaine Finance");
        Assert.assertContains(finance.expertPersona(), "Économiste", "Persona Économiste");

        // 10. Médecine & Neurosciences
        DomainExtractor.DomainInfo medecine = DomainExtractor.analyser("Fonctionnement des neurones et du cerveau humain");
        Assert.assertEquals("Médecine & Sciences Biomédicales", medecine.domainName(), "Domaine Médecine");
        Assert.assertContains(medecine.expertPersona(), "Médecin", "Persona Médecin");
    }

    public void testExtractionSujetPivotDynamique() {
        DomainExtractor.DomainInfo info = DomainExtractor.analyser("Explique le fonctionnement de la blockchain");
        Assert.assertNotNull(info.extractedTopic(), "Sujet pivot non null");
        Assert.assertFalse(info.extractedTopic().isEmpty(), "Sujet pivot non vide");
    }

    public void testPersonaFallbackSurPromptSansDomaine() {
        DomainExtractor.DomainInfo info = DomainExtractor.analyser("Fais un tri");
        Assert.assertNotNull(info, "Info non null");
        Assert.assertFalse(info.expertPersona().isEmpty(), "Persona fallback non vide");
    }

    public void testEntreeVideEtNull() {
        DomainExtractor.DomainInfo vide = DomainExtractor.analyser("");
        Assert.assertFalse(vide.isDomainIdentified(), "Vide non identifié");

        DomainExtractor.DomainInfo nul = DomainExtractor.analyser(null);
        Assert.assertFalse(nul.isDomainIdentified(), "Null non identifié");
    }
}
