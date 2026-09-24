package test.cli;

import cli.CliArgs;
import cli.CliParser;
import cli.FormatSortie;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;
import test.framework.Test;

import java.nio.file.Path;

// Branchement de -o/--output : choix du format, fichier cible et rendu
public class FormatSortieTest {

    private static CliArgs avecSortie(String valeur) {
        return CliParser.parse(new String[]{"-o", valeur});
    }

    @Test
    public void testSansOptionLeFormatEstMarkdown() {
        Assert.assertEquals(FormatSortie.MD, FormatSortie.depuis(CliParser.parse(new String[]{"-i", "x"})), "Defaut Markdown");
        Assert.assertEquals(FormatSortie.MD, FormatSortie.depuis(null), "Options nulles");
        Assert.assertTrue(FormatSortie.fichierCible(null).isEmpty(), "Pas de fichier sans -o");
    }

    @Test
    public void testMotsClesInsensiblesALaCasse() {
        Assert.assertEquals(FormatSortie.TXT, FormatSortie.depuis(avecSortie("TXT")), "txt");
        Assert.assertEquals(FormatSortie.MD, FormatSortie.depuis(avecSortie("markdown")), "markdown");
        Assert.assertEquals(FormatSortie.JSON, FormatSortie.depuis(avecSortie("Json")), "json");
        Assert.assertTrue(FormatSortie.fichierCible(avecSortie("json")).isEmpty(), "Un mot-cle n'est pas un fichier");
    }

    @Test
    public void testExtensionDuFichierDecideDuFormat() {
        Assert.assertEquals(FormatSortie.JSON, FormatSortie.depuis(avecSortie("out/prompt.JSON")), ".json");
        Assert.assertEquals(FormatSortie.TXT, FormatSortie.depuis(avecSortie("prompt.txt")), ".txt");
        Assert.assertEquals(FormatSortie.MD, FormatSortie.depuis(avecSortie("prompt.md")), ".md");
        Assert.assertEquals(FormatSortie.MD, FormatSortie.depuis(avecSortie("prompt")), "Sans extension");
        Assert.assertEquals(Path.of("prompt.txt"), FormatSortie.fichierCible(avecSortie("prompt.txt")).orElse(null), "Chemin conserve");
    }

    @Test
    public void testVersTexteBrutRetireLeBalisage() {
        String md = "# Titre\n## Sous-titre\n- **Gras** et *italique* avec `code`\n```java\nint x = 2 * 3;\n```\n* puce";
        String txt = FormatSortie.versTexteBrut(md);
        Assert.assertEquals("Titre\nSous-titre\n- Gras et italique avec code\nint x = 2 * 3;\n* puce", txt, "Texte brut");
        Assert.assertEquals("", FormatSortie.versTexteBrut(null), "Nul");
    }

    @Test
    public void testRenduSelonLeFormat() {
        PromptProfile profil = Lemmatizer.analyser("Explique la photosynthèse");
        String prompt = "## Mission\n**Expliquer** la photosynthèse";
        Assert.assertEquals(prompt, FormatSortie.MD.rendre(profil, prompt, null), "MD inchange");
        Assert.assertEquals("Mission\nExpliquer la photosynthèse", FormatSortie.TXT.rendre(profil, prompt, null), "TXT nettoye");
        Assert.assertContains(FormatSortie.JSON.rendre(profil, prompt, null), "\"meta_prompt\":", "JSON structure");
    }
}
