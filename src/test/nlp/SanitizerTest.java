package test.nlp;

import nlp.Sanitzer;
import test.framework.Assert;
import test.framework.Test;

public class SanitizerTest {

    @Test
    public void testNettoyageBasiqueEtMinuscules() {
        String input = "Bonjour LE MONDE ! Comment allez-vous ?";
        String result = Sanitzer.nettoyerPrompt(input);
        Assert.assertEquals("bonjour le monde comment allez vous", result, "Le texte doit être passé en minuscules et sans ponctuation");
    }

    @Test
    public void testRestaurationElisionsFormesVerbales() {
        Assert.assertContains(Sanitzer.restaurerElisions("cest super"), "c'est", "cest -> c'est");
        Assert.assertContains(Sanitzer.restaurerElisions("cetait facile"), "c'était", "cetait -> c'était");
        Assert.assertContains(Sanitzer.restaurerElisions("sest passe vite"), "s'est", "sest -> s'est");
        Assert.assertContains(Sanitzer.restaurerElisions("nest pas la"), "n'est", "nest -> n'est");
        Assert.assertContains(Sanitzer.restaurerElisions("jai fini"), "j'ai", "jai -> j'ai");
        Assert.assertContains(Sanitzer.restaurerElisions("javais faim"), "j'avais", "javais -> j'avais");
        Assert.assertContains(Sanitzer.restaurerElisions("jaime coder"), "j'aime", "jaime -> j'aime");
    }

    @Test
    public void testRestaurationElisionsArticlesEtPronoms() {
        Assert.assertContains(Sanitzer.restaurerElisions("lavion decolle"), "l'avion", "lavion -> l'avion");
        Assert.assertContains(Sanitzer.restaurerElisions("lordinateur tourne"), "l'ordinateur", "lordinateur -> l'ordinateur");
        Assert.assertContains(Sanitzer.restaurerElisions("lart du prompt"), "l'art", "lart -> l'art");
        Assert.assertContains(Sanitzer.restaurerElisions("lunivers est vaste"), "l'univers", "lunivers -> l'univers");
        Assert.assertContains(Sanitzer.restaurerElisions("lhomme moderne"), "l'homme", "lhomme -> l'homme");
        Assert.assertContains(Sanitzer.restaurerElisions("limage est claire"), "l'image", "limage -> l'image");
        Assert.assertContains(Sanitzer.restaurerElisions("lhistoire se repete"), "l'histoire", "lhistoire -> l'histoire");
    }

    @Test
    public void testRestaurationElisionsPrepositionsEtConjonctions() {
        Assert.assertContains(Sanitzer.restaurerElisions("besoin dun outil"), "d'un", "dun -> d'un");
        Assert.assertContains(Sanitzer.restaurerElisions("arrivee dune femme"), "d'une", "dune -> d'une");
        Assert.assertContains(Sanitzer.restaurerElisions("manque dargent"), "d'argent", "dargent -> d'argent");
        Assert.assertContains(Sanitzer.restaurerElisions("il faut quon fasse"), "qu'on", "quon -> qu'on");
        Assert.assertContains(Sanitzer.restaurerElisions("je sais quil vient"), "qu'il", "quil -> qu'il");
        Assert.assertContains(Sanitzer.restaurerElisions("lorsquon code"), "lorsqu'on", "lorsquon -> lorsqu'on");
        Assert.assertContains(Sanitzer.restaurerElisions("puisquon est la"), "puisqu'on", "puisquon -> puisqu'on");
        Assert.assertContains(Sanitzer.restaurerElisions("jusqua demain"), "jusqu'à", "jusqua -> jusqu'à");
    }

    @Test
    public void testRestaurationExpressionsComposees() {
        Assert.assertContains(Sanitzer.restaurerElisions("yatil un pilote ?"), "y a-t-il", "yatil -> y a-t-il");
        Assert.assertContains(Sanitzer.restaurerElisions("ya beaucoup de monde"), "il y a", "ya -> il y a");
        Assert.assertContains(Sanitzer.restaurerElisions("sil vous plait"), "s'il vous plaît", "sil vous plait -> s'il vous plaît");
        Assert.assertContains(Sanitzer.restaurerElisions("aujourdhui il fait beau"), "aujourd'hui", "aujourdhui -> aujourd'hui");
        Assert.assertContains(Sanitzer.restaurerElisions("quelquun arrive"), "quelqu'un", "quelquun -> quelqu'un");
    }

    @Test
    public void testNormalisationApostrophesTypographiques() {
        String input = "l’avion et d’autres d`espaces";
        String res = Sanitzer.restaurerElisions(input);
        Assert.assertFalse(res.contains("’"), "Les apostrophes courbées doivent être normalisées");
        Assert.assertFalse(res.contains("`"), "Les backticks typographiques doivent être normalisés");
        Assert.assertContains(res, "l'avion", "L'apostrophe standard doit être présente");
    }

    @Test
    public void testPreservationSymbolesCPlusPlusEtCSharp() {
        String input = "Je programme en C++ et en C# avec .NET";
        String promptNettoye = Sanitzer.nettoyerPrompt(input);
        Assert.assertContains(promptNettoye, "c++", "Les symboles C++ doivent être préservés");
        Assert.assertContains(promptNettoye, "c#", "Les symboles C# doivent être préservés");
    }

    @Test
    public void testNettoyerEtFormaterTexteAvecCapitalisation() {
        String input = "cest super. lavion vole! comment ca va? oui.";
        String formate = Sanitzer.nettoyerEtFormaterTexte(input);
        Assert.assertContains(formate, "C'est", "La première lettre de phrase doit être en majuscule");
        Assert.assertContains(formate, "L'avion", "La lettre après point d'exclamation doit être en majuscule");
    }

    @Test
    public void testEdgeCasesEspacesNullVide() {
        Assert.assertEquals("", Sanitzer.nettoyerPrompt(""), "Chaîne vide -> vide");
        Assert.assertEquals("", Sanitzer.nettoyerPrompt(null), "Null -> vide");
        Assert.assertEquals("", Sanitzer.nettoyerPrompt("     \n\t   "), "Blancs -> vide");
        Assert.assertEquals("", Sanitzer.restaurerElisions(""), "Elisions sur vide -> vide");
        Assert.assertEquals("", Sanitzer.restaurerElisions(null), "Elisions sur null -> vide");
        Assert.assertEquals("", Sanitzer.nettoyerEtFormaterTexte(""), "Formatage sur vide -> vide");
        Assert.assertEquals("", Sanitzer.nettoyerEtFormaterTexte(null), "Formatage sur null -> vide");
    }
}
