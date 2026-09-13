package test.nlp;

import nlp.TechStackDetector;
import test.framework.Assert;

import java.util.List;
import java.util.Optional;

public class TechStackDetectorTest {

    public void testDetectionTechnologiesJavaSpringBoot() {
        String prompt = "Je veux créer une API avec Spring Boot et du Java 21";
        List<String> techs = TechStackDetector.detecterTechnologies(prompt);
        Assert.assertContainsElement(techs, "Java", "Le groupe technologique Java doit être détecté");
    }

    public void testDetectionTechnologiesFrontendReactTailwind() {
        String prompt = "Fais un composant bouton responsive avec React et Tailwind CSS";
        List<String> techs = TechStackDetector.detecterTechnologies(prompt);
        Assert.assertContainsElement(techs, "JavaScript / TypeScript", "Le groupe JS/TS (React) doit être détecté");
        Assert.assertContainsElement(techs, "HTML / CSS / UI Design", "Le groupe UI/CSS (Tailwind) doit être détecté");
    }

    public void testDetectionTechnologiesDatabaseSQL() {
        String prompt = "Rédige une requête PostgreSQL avec index pour optimiser la table";
        List<String> techs = TechStackDetector.detecterTechnologies(prompt);
        Assert.assertContainsElement(techs, "SQL", "SQL doit être détecté");
    }

    public void testDetectionLangueCibleTraduction() {
        String prompt = "Traduis cette documentation en anglais";
        Optional<String> targetLang = TechStackDetector.detecterLangueCibleTraduction(prompt);
        Assert.assertTrue(targetLang.isPresent(), "La langue cible doit être détectée");
        Assert.assertEquals("Anglais (EN)", targetLang.get(), "La langue cible doit être l'anglais");

        String promptEspagnol = "Traduis ce texte vers l'espagnol s'il te plaît";
        Optional<String> targetEs = TechStackDetector.detecterLangueCibleTraduction(promptEspagnol);
        Assert.assertTrue(targetEs.isPresent(), "L'espagnol doit être détecté");
        Assert.assertEquals("Espagnol (ES)", targetEs.get(), "La langue cible doit être l'espagnol");
    }

    public void testDetectionContexteSpecialAcademique() {
        String prompt = "Rédige une dissertation niveau thèse de doctorat sur l'astrophysique";
        Optional<String> ctx = TechStackDetector.detecterContexteSpecial(prompt);
        Assert.assertTrue(ctx.isPresent(), "Un contexte académique doit être détecté");
        Assert.assertContains(ctx.get(), "Universitaire", "Le contexte doit mentionner le niveau universitaire");
    }
}
