package test.corpus;

import corpus.Corpus;
import corpus.RechercheProgressive;
import test.framework.Assert;
import test.framework.Test;

import java.util.List;

// Recherche des meilleurs prompts proches par filtrage progressif sur les mots
public class RechercheProgressiveTest {

    private static Corpus.Prompt p(String id, String langue, String texte, Double qualite) {
        return new Corpus.Prompt(id, "test", langue, texte, qualite);
    }

    private static final List<Corpus.Prompt> CORPUS = List.of(
            p("1", "fr", "Quelles croquettes choisir pour mon chaton de 3 mois ?", 0.9),
            p("2", "fr", "Mon chat refuse ses croquettes depuis hier, que faire ?", 0.5),
            p("3", "fr", "Comment apprendre la litière à un chaton ?", null),
            p("4", "fr", "Les chats peuvent-ils manger du thon ?", null),
            p("5", "fr", "Visite du château de Versailles : que voir ?", null),
            p("6", "fr", "Comment utiliser ChatGPT pour réviser ?", null),
            p("7", "en", "My cat hates the new cat food, what should I do?", null),
            p("8", "fr", "Idée de recette avec des croquettes de poulet", null),
            p("9", "fr", "Écris un poème sur la mer", null),
            p("10", "fr", "Comment dresser un chien ?", null),
            p("11", "fr", "Quel vélo acheter pour la ville ?", null));

    // chat, chats, chaton, chatons : une seule racine ; ni chateau ni chatgpt
    @Test
    public void testVariantesDUnMot() {
        String chat = RechercheProgressive.racine("chat");
        Assert.assertEquals(chat, RechercheProgressive.racine("chats"), "Pluriel");
        Assert.assertEquals(chat, RechercheProgressive.racine("chaton"), "Diminutif");
        Assert.assertEquals(chat, RechercheProgressive.racine("Chatons"), "Diminutif pluriel, majuscule");
        Assert.assertEquals(RechercheProgressive.racine("croquette"), RechercheProgressive.racine("croquettes"), "Pluriel");
        Assert.assertFalse(chat.equals(RechercheProgressive.racine("château")), "Chateau n'est pas un chat");
        Assert.assertFalse(chat.equals(RechercheProgressive.racine("ChatGPT")), "ChatGPT n'est pas un chat");
    }

    // croquette -> 3 prompts, puis chat -> 2 : le mot le plus precis d'abord
    @Test
    public void testFiltrageProgressif() {
        RechercheProgressive.Resultat r = new RechercheProgressive(CORPUS)
                .rechercher("je cherche des croquettes pour mon chaton", "FR", 5);

        Assert.assertEquals(RechercheProgressive.racine("croquette"), r.etapes().get(0).racine(), "Le plus precis d'abord");
        Assert.assertEquals(3, r.etapes().get(0).apres(), "3 prompts sur les croquettes");
        Assert.assertEquals(RechercheProgressive.racine("chat"), r.etapes().get(1).racine(), "Puis le sujet");
        Assert.assertEquals(2, r.candidats(), "Chat + croquette");
        Assert.assertEquals("1", r.meilleurs().get(0).id(), "Le mieux note en premier");
        Assert.assertTrue(r.meilleurs().stream().noneMatch(x -> x.id().equals("8")), "Croquettes de poulet ecartees");
    }

    // Un mot qui viderait la liste est saute, la recherche continue avec les suivants
    @Test
    public void testMotSansResultatSaute() {
        RechercheProgressive.Resultat r = new RechercheProgressive(CORPUS)
                .rechercher("croquettes pour chaton, et un vélo", "FR", 5);

        Assert.assertTrue(r.etapes().stream().anyMatch(e -> !e.retenue() && e.racine().equals("velo")),
                "Velo saute : aucun prompt ne parle de chat, de croquettes et de velo");
        List<String> premiers = r.meilleurs().stream().limit(2).map(Corpus.Prompt::id).sorted().toList();
        Assert.assertEquals(List.of("1", "2"), premiers, "Chat + croquette devant le prompt qui ne parle que de velo");
    }

    // En anglais, "chat" veut dire discussion : on ne cherche que dans la langue du prompt
    @Test
    public void testMemeLangueSeulement() {
        RechercheProgressive.Resultat r = new RechercheProgressive(CORPUS).rechercher("croquettes pour chat", "fr", 10);
        Assert.assertTrue(r.meilleurs().stream().allMatch(x -> x.langue().equals("fr")), "Prompts francais seulement");
    }

    @Test
    public void testAucunMotConnu() {
        RechercheProgressive.Resultat r = new RechercheProgressive(CORPUS).rechercher("xyzzy plugh", "fr", 5);
        Assert.assertEquals(0, r.candidats(), "Aucun resultat");
        Assert.assertTrue(r.meilleurs().isEmpty(), "Liste vide");
    }
}
