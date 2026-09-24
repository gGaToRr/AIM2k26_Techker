package test.util;

import test.framework.Assert;
import test.framework.Test;
import util.Json;

// Lecture et ecriture JSON minimales utilisees pour dialoguer avec l'extension
public class JsonTest {

    @Test
    public void testExtraireChaineSimple() {
        Assert.assertEquals("fais un tri", Json.extraireChaine("{\"action\":\"ameliorer\",\"prompt\":\"fais un tri\"}", "prompt"),
                "Valeur lue");
    }

    // Un prompt peut contenir n'importe quel caractere : ils doivent tous revenir intacts
    @Test
    public void testExtraireChaineAvecEchappements() {
        String json = "{\"prompt\":\"dit \\\"bonjour\\\"\\nligne 2\\\\fin \\u00e9t\\u00e9 \\ud83d\\ude80\"}";
        Assert.assertEquals("dit \"bonjour\"\nligne 2\\fin été 🚀", Json.extraireChaine(json, "prompt"),
                "Guillemets, saut de ligne, antislash, accents et emoji");
    }

    @Test
    public void testExtraireChaineAbsenteOuMalFormee() {
        Assert.assertNull(Json.extraireChaine("{\"action\":\"x\"}", "prompt"), "Cle absente");
        Assert.assertNull(Json.extraireChaine("{\"prompt\":\"non termine", "prompt"), "Chaine non terminee");
        Assert.assertNull(Json.extraireChaine(null, "prompt"), "JSON nul");
    }

    // Ce qui est ecrit par chaine() doit se relire a l'identique
    @Test
    public void testAllerRetour() {
        String texte = "Role : \"expert\"\n\tTache : a\\b";
        Assert.assertEquals(texte, Json.extraireChaine("{\"t\":" + Json.chaine(texte) + "}", "t"), "Aller-retour");
    }

    // Un prompt peut contenir des caracteres de controle : le JSON produit doit rester valide
    @Test
    public void testCaracteresDeControleEchappes() {
        String texte = "saut\fde page, echap\u001b, separateur\u2028fin";
        String json = "{\"t\":" + Json.chaine(texte) + "}";
        Assert.assertFalse(json.chars().anyMatch(c -> c < 0x20 || c == 0x2028), "Aucun caractere de controle brut");
        Assert.assertEquals(texte, Json.extraireChaine(json, "t"), "Aller-retour exact");
    }
}
