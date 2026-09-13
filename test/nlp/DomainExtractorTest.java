package test.nlp;

import nlp.DomainExtractor;
import test.framework.Assert;

public class DomainExtractorTest {

    public void testExtractionDomaineMusique() {
        String prompt = "Comment accorder une guitare acoustique et jouer des accords de jazz ?";
        DomainExtractor.DomainInfo info = DomainExtractor.analyser(prompt);
        Assert.assertTrue(info.isDomainIdentified(), "Le domaine doit être identifié");
        Assert.assertEquals("Musique & Lutherie", info.domainName(), "Le domaine doit être Musique & Lutherie");
        Assert.assertContainsIgnoreCase(info.expertPersona(), "Musicien", "Le persona doit faire référence à un musicien");
    }

    public void testExtractionDomaineCuisine() {
        String prompt = "Donne-moi la recette de la tarte au citron meringuée avec temps de cuisson";
        DomainExtractor.DomainInfo info = DomainExtractor.analyser(prompt);
        Assert.assertTrue(info.isDomainIdentified(), "Le domaine cuisine doit être identifié");
        Assert.assertEquals("Gastronomie & Arts Culinaires", info.domainName(), "Le domaine doit être Gastronomie");
        Assert.assertContainsIgnoreCase(info.expertPersona(), "Chef", "Le persona doit faire référence à un Chef");
    }

    public void testExtractionDomaineAeronautique() {
        String prompt = "Explique le fonctionnement du turboréacteur d'un avion supersonique";
        DomainExtractor.DomainInfo info = DomainExtractor.analyser(prompt);
        Assert.assertTrue(info.isDomainIdentified(), "Le domaine aéronautique doit être identifié");
        Assert.assertEquals("Aéronautique & Systèmes de Transport", info.domainName(), "Le domaine doit être Aéronautique");
        Assert.assertContainsIgnoreCase(info.expertPersona(), "Ingénieur", "Le persona doit faire référence à un Ingénieur");
    }

    public void testExtractionDomaineFinance() {
        String prompt = "Comment construire un portefeuille d'actions et obligations avec bon rendement ?";
        DomainExtractor.DomainInfo info = DomainExtractor.analyser(prompt);
        Assert.assertTrue(info.isDomainIdentified(), "Le domaine finance doit être identifié");
        Assert.assertEquals("Économie & Marchés Financiers", info.domainName(), "Le domaine doit être Économie & Marchés Financiers");
    }

    public void testExtractionDomaineDroit() {
        String prompt = "Quelles sont les clauses obligatoires dans un contrat de travail ?";
        DomainExtractor.DomainInfo info = DomainExtractor.analyser(prompt);
        Assert.assertTrue(info.isDomainIdentified(), "Le domaine droit doit être identifié");
        Assert.assertEquals("Droit & Réglementation", info.domainName(), "Le domaine doit être Droit & Réglementation");
    }

    public void testExtractionDomaineInconnuFallback() {
        String prompt = "Fais un résumé";
        DomainExtractor.DomainInfo info = DomainExtractor.analyser(prompt);
        Assert.assertNotNull(info, "Les infos de domaine ne doivent pas être null");
    }
}
