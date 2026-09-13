package test.nlp;

import nlp.TechStackDetector;
import test.framework.Assert;

import java.util.List;
import java.util.Optional;

public class TechStackDetectorTest {

    public void testDetectionToutesFamillesTechnologiques() {
        // C++ et C#
        List<String> cpp = TechStackDetector.detecterTechnologies("Implémente un algorithme en C++ avec CMake");
        Assert.assertContainsElement(cpp, "C++", "C++ détecté");

        List<String> csharp = TechStackDetector.detecterTechnologies("Crée une application WPF en C# avec .NET");
        Assert.assertContainsElement(csharp, "C#", "C# détecté");

        // Java
        List<String> java = TechStackDetector.detecterTechnologies("Projet Spring Boot avec Hibernate et Maven");
        Assert.assertContainsElement(java, "Java", "Java détecté");

        // Python
        List<String> python = TechStackDetector.detecterTechnologies("Script d'entraînement en PyTorch et TensorFlow");
        Assert.assertContainsElement(python, "Python", "Python détecté");

        // JavaScript / TypeScript
        List<String> js = TechStackDetector.detecterTechnologies("Application front en Next.js avec React et TypeScript");
        Assert.assertContainsElement(js, "JavaScript / TypeScript", "JS/TS détecté");

        // Rust
        List<String> rust = TechStackDetector.detecterTechnologies("Programme multi-thread avec Rust et Tokio");
        Assert.assertContainsElement(rust, "Rust", "Rust détecté");

        // Go
        List<String> go = TechStackDetector.detecterTechnologies("Microservice en Golang avec Gin et des goroutines");
        Assert.assertContainsElement(go, "Go", "Go détecté");

        // SQL / Database
        List<String> sql = TechStackDetector.detecterTechnologies("Schéma PostgreSQL avec MongoDB et Redis pour le cache");
        Assert.assertContainsElement(sql, "SQL", "SQL détecté");

        // DevOps
        List<String> devops = TechStackDetector.detecterTechnologies("Déploiement Docker sur un cluster Kubernetes avec Terraform");
        Assert.assertContainsElement(devops, "DevOps / Cloud", "DevOps détecté");

        // Game Dev
        List<String> gamedev = TechStackDetector.detecterTechnologies("Jeu vidéo développé avec Unreal Engine et Godot");
        Assert.assertContainsElement(gamedev, "Unity / Game Dev", "Game Dev détecté");
    }

    public void testDetectionLanguesCiblesMultiples() {
        // Anglais
        Optional<String> en = TechStackDetector.detecterLangueCibleTraduction("Traduis ce document en anglais");
        Assert.assertTrue(en.isPresent(), "EN présent");
        Assert.assertEquals("Anglais (EN)", en.get(), "Anglais");

        // Espagnol
        Optional<String> es = TechStackDetector.detecterLangueCibleTraduction("Traduis ce texte vers l'espagnol");
        Assert.assertTrue(es.isPresent(), "ES présent");
        Assert.assertEquals("Espagnol (ES)", es.get(), "Espagnol");

        // Allemand
        Optional<String> de = TechStackDetector.detecterLangueCibleTraduction("Translate this manual into german");
        Assert.assertTrue(de.isPresent(), "DE présent");
        Assert.assertEquals("Allemand (DE)", de.get(), "Allemand");

        // Japonais
        Optional<String> ja = TechStackDetector.detecterLangueCibleTraduction("Traduire vers le japonais");
        Assert.assertTrue(ja.isPresent(), "JA présent");
        Assert.assertEquals("Japonais (JA)", ja.get(), "Japonais");

        // Aucun contexte de traduction
        Optional<String> none = TechStackDetector.detecterLangueCibleTraduction("Explique la recette du pain");
        Assert.assertFalse(none.isPresent(), "Pas de langue de traduction pour un prompt culinaire");
    }

    public void testDetectionContextesSpeciauxEcolesEtRecherche() {
        Optional<String> epitech = TechStackDetector.detecterContexteSpecial("Projet epitech en C");
        Assert.assertTrue(epitech.isPresent(), "Epitech présent");
        Assert.assertContains(epitech.get(), "Epitech", "Contexte Epitech");

        Optional<String> ecole42 = TechStackDetector.detecterContexteSpecial("Exercice pour l'école 42");
        Assert.assertTrue(ecole42.isPresent(), "42 présent");
        Assert.assertContains(ecole42.get(), "42", "Contexte 42");

        Optional<String> these = TechStackDetector.detecterContexteSpecial("Rédaction de ma thèse de doctorat");
        Assert.assertTrue(these.isPresent(), "Thèse présente");
        Assert.assertContains(these.get(), "Thèse", "Contexte Thèse");
    }

    public void testEntreesVidesEtSansTechnologies() {
        Assert.assertTrue(TechStackDetector.detecterTechnologies("").isEmpty(), "Vide -> liste vide");
        Assert.assertTrue(TechStackDetector.detecterTechnologies(null).isEmpty(), "Null -> liste vide");
        Assert.assertTrue(TechStackDetector.detecterTechnologies("Une simple phrase sans informatique").isEmpty(), "Texte sans tech -> liste vide");
    }
}
