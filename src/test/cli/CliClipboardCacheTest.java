package test.cli;

import cli.CliClipboard;
import test.framework.Assert;
import test.framework.Test;

// Tests du cache de detection des utilitaires systeme de presse-papiers (Issue #67)
public class CliClipboardCacheTest {

    @Test
    public void testCommandeExisteNeSondeQuUneSeuleFoisParNom() {
        CliClipboard.viderCacheCommandes();

        boolean premier = CliClipboard.commandeExiste("xclip");
        int apresPremier = CliClipboard.nombreDeSondagesSysteme();

        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(premier, CliClipboard.commandeExiste("xclip"), "Resultat stable a chaque appel");
        }

        Assert.assertEquals(1, apresPremier, "Le premier appel lance un unique sondage systeme");
        Assert.assertEquals(1, CliClipboard.nombreDeSondagesSysteme(),
                "Les 20 appels suivants ne relancent aucun process");
    }

    @Test
    public void testCacheDistinctParNomDeCommande() {
        CliClipboard.viderCacheCommandes();

        CliClipboard.commandeExiste("xclip");
        CliClipboard.commandeExiste("xsel");
        CliClipboard.commandeExiste("wl-copy");
        CliClipboard.commandeExiste("xclip");
        CliClipboard.commandeExiste("xsel");

        Assert.assertEquals(3, CliClipboard.nombreDeSondagesSysteme(),
                "Un sondage par nom distinct, pas un par appel");
    }

    @Test
    public void testCommandeNulleOuVideNeSondeRien() {
        CliClipboard.viderCacheCommandes();

        Assert.assertFalse(CliClipboard.commandeExiste(null), "Commande nulle refusee");
        Assert.assertFalse(CliClipboard.commandeExiste("   "), "Commande vide refusee");
        Assert.assertEquals(0, CliClipboard.nombreDeSondagesSysteme(), "Aucun process lance pour une saisie vide");
    }

    // Une commande absente doit etre mise en cache aussi : sinon chaque copie la resonde
    @Test
    public void testResultatNegatifEgalementMisEnCache() {
        CliClipboard.viderCacheCommandes();

        String inexistante = "commande_qui_n_existe_pas_987654";
        Assert.assertFalse(CliClipboard.commandeExiste(inexistante), "Commande inexistante");
        Assert.assertFalse(CliClipboard.commandeExiste(inexistante), "Toujours inexistante");
        Assert.assertFalse(CliClipboard.commandeExiste(inexistante), "Toujours inexistante");

        Assert.assertEquals(1, CliClipboard.nombreDeSondagesSysteme(),
                "Un resultat negatif est mis en cache au meme titre qu'un positif");
    }
}
