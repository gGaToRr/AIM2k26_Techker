package test.nlp;

import nlp.Sanitzer;
import test.framework.Assert;

public class SanitizerTest {

    public void testNettoyageBasiqueEtMinuscules() {
        String input = "Bonjour LE MONDE ! Comment allez-vous ?";
        String result = Sanitzer.nettoyerPrompt(input);
        Assert.assertContains(result, "bonjour le monde", "Le texte doit être passé en minuscules et nettoyé");
    }

    public void testRestaurationElisionLavion() {
        String input = "lavion vole dans le ciel";
        String result = Sanitzer.restaurerElisions(input);
        Assert.assertContains(result, "l'avion", "L'élision 'lavion' doit être restaurée en 'l'avion'");
    }

    public void testRestaurationElisionCest() {
        String input = "cest une excellente idee";
        String result = Sanitzer.restaurerElisions(input);
        Assert.assertContains(result, "c'est", "L'élision 'cest' doit être restaurée en 'c'est'");
    }

    public void testRestaurationElisionDun() {
        String input = "il a besoin dun outil rapide";
        String result = Sanitzer.restaurerElisions(input);
        Assert.assertContains(result, "d'un", "L'élision 'dun' doit être restaurée en 'd'un'");
    }

    public void testRestaurationElisionQuon() {
        String input = "il faut quon fasse attention";
        String result = Sanitzer.restaurerElisions(input);
        Assert.assertContains(result, "qu'on", "L'élision 'quon' doit être restaurée en 'qu'on'");
    }

    public void testPreservationCodeSourceEtApostrophes() {
        String input = "SELECT * FROM user WHERE id = '123'";
        String result = Sanitzer.nettoyerPrompt(input);
        Assert.assertContains(result, "select", "Le code SQL doit être normalisé sans corruption");
    }

    public void testNormalisationEspacesMultiples() {
        String input = "trop    des      espaces      ici";
        String result = Sanitzer.nettoyerPrompt(input);
        Assert.assertEquals("trop des espaces ici", result, "Les espaces multiples doivent être réduits à un seul");
    }

    public void testEntreeVideOuNull() {
        Assert.assertEquals("", Sanitzer.nettoyerPrompt(""), "Une chaîne vide doit retourner une chaîne vide");
        Assert.assertEquals("", Sanitzer.nettoyerPrompt(null), "Une entrée null doit retourner une chaîne vide");
    }
}
