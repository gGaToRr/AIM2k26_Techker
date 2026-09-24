package test.nlp;

import nlp.Racines;
import nlp.ThemeClassifier;
import test.framework.Assert;
import test.framework.Test;

import java.util.List;

// Themes des prompts : graines et vocabulaire appris (src/genPrompt/themes/)
public class ThemeClassifierTest {

    private static List<String> themes(String prompt, String langue) {
        return ThemeClassifier.themesProches(prompt, langue);
    }

    // Cas verifies sur le vocabulaire appris a partir du corpus
    @Test
    public void testThemesDeVraisPrompts() {
        Assert.assertEquals("cuisine", themes("donne moi une recette de lasagnes végétariennes pour 6 personnes", "FR").get(0), "Cuisine");
        Assert.assertEquals("voyage", themes("plan a 5 day trip to Japan in spring", "EN").get(0), "Voyage");
        Assert.assertEquals("emploi_carriere", themes("rédige une lettre de motivation pour un poste de comptable", "FR").get(0), "Emploi");
        Assert.assertEquals("finance", themes("quelle est la meilleure façon d'investir 10000 euros", "FR").get(0), "Finance");
        Assert.assertEquals("animaux", themes("mon chat vomit après avoir mangé ses croquettes", "FR").get(0), "Animaux");
    }

    // Une technologie reconnue suffit a designer la programmation, meme sans autre indice
    @Test
    public void testTechnologieDesigneLaProgrammation() {
        Assert.assertEquals("programmation", themes("how do I fix a NullPointerException in my Spring Boot controller", "EN").get(0),
                "Spring Boot");
    }

    // "chat" est un animal en francais, une discussion en anglais ; "chat gpt" n'est jamais un animal
    @Test
    public void testGrainesPropresALaLangue() {
        Assert.assertFalse(themes("can you chat with me about my day", "EN").contains("animaux"), "chat anglais");
        Assert.assertFalse(Racines.de("tu es quelle version de chat gtp ?").contains(Racines.racine("chat")), "chat gtp");
    }

    @Test
    public void testSansIndiceDivers() {
        Assert.assertEquals(List.of(ThemeClassifier.DIVERS), themes("bonjour", "FR"), "Divers");
        Assert.assertEquals(List.of(ThemeClassifier.DIVERS), themes("", "FR"), "Vide");
    }

    // Au plus 3 themes, tous assez proches du meilleur
    @Test
    public void testThemesProchesBornes() {
        List<ThemeClassifier.ScoreTheme> scores = List.of(
                new ThemeClassifier.ScoreTheme("a", "A", 10), new ThemeClassifier.ScoreTheme("b", "B", 7),
                new ThemeClassifier.ScoreTheme("c", "C", 6.5), new ThemeClassifier.ScoreTheme("d", "D", 6.2),
                new ThemeClassifier.ScoreTheme("e", "E", 2));
        Assert.assertEquals(List.of("a", "b", "c"), ThemeClassifier.themesProches(scores), "3 au plus, >= 60 % du meilleur");
    }

    @Test
    public void testRacines() {
        Assert.assertEquals("spring", Racines.racine("spring"), "spring n'est pas spr");
        Assert.assertEquals("mois", Racines.racine("mois"), "mois n'est pas moi");
        Assert.assertEquals(Racines.racine("chat"), Racines.racine("chatons"), "chatons");
    }
}
