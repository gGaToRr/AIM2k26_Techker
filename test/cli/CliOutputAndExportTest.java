package test.cli;

import cli.CliArgs;
import cli.CliClipboard;
import cli.CliParser;
import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// Tests d'intégration du formatage de sortie et des mécanismes d'export (Issue #32)
public class CliOutputAndExportTest {

    // Test de la structure et des champs obligatoires du JSON
    public void testJsonOutputStructureAndFields() {
        PromptProfile profile = Lemmatizer.analyser("Créer une application React avec Tailwind CSS et TypeScript");
        CliArgs options = CliParser.parse(new String[]{"-o", "json"});

        String prompt = MetaPromptEngine.genererPromptOptimise(profile, options);
        String json = MetaPromptEngine.genererExportJson(profile, prompt, options);

        Assert.assertNotNull(json, "Le JSON généré ne doit pas être null");
        Assert.assertContains(json, "\"project\":", "Doit contenir la clé project");
        Assert.assertContains(json, "\"version\":", "Doit contenir la clé version");
        Assert.assertContains(json, "\"prompt_profile\":", "Doit contenir l'objet prompt_profile");
        Assert.assertContains(json, "\"raw_text\":", "Doit contenir raw_text");
        Assert.assertContains(json, "\"language\":", "Doit contenir language");
        Assert.assertContains(json, "\"archetype\":", "Doit contenir archetype");
        Assert.assertContains(json, "\"quality_score\":", "Doit contenir quality_score");
        Assert.assertContains(json, "\"estimated_tokens\":", "Doit contenir estimated_tokens");
        Assert.assertContains(json, "\"meta_prompt\":", "Doit contenir meta_prompt");
    }

    // Test de l'échappement des caractères spéciaux dans le JSON (guillemets, retours à la ligne)
    public void testJsonSpecialCharactersEscaping() {
        PromptProfile profile = Lemmatizer.analyser("Corrige ce code: if (x == \"test\") { alert('error'); }");
        CliArgs options = CliParser.parse(new String[]{"-o", "json"});

        String prompt = MetaPromptEngine.genererPromptOptimise(profile, options);
        String json = MetaPromptEngine.genererExportJson(profile, prompt, options);

        Assert.assertNotContains(json, "\n  \"meta_prompt\": \"\n", "Les retours à la ligne doivent être échappés par \\n");
        Assert.assertContains(json, "\\\"", "Les guillemets internes doivent être échappés");
    }

    // Test d'export vers un fichier Markdown (.md)
    public void testExportToFileMarkdown() throws IOException {
        File tempMd = File.createTempFile("export_test_", ".md");
        tempMd.deleteOnExit();

        PromptProfile profile = Lemmatizer.analyser("Explique la théorie de la relativité générale");
        CliArgs options = CliParser.parse(new String[]{"-o", tempMd.getAbsolutePath()});

        String prompt = MetaPromptEngine.genererPromptOptimise(profile, options);
        Files.writeString(tempMd.toPath(), prompt);

        Assert.assertTrue(tempMd.exists(), "Le fichier exporté doit exister");
        String fileContent = Files.readString(tempMd.toPath());
        Assert.assertEquals(prompt, fileContent, "Le contenu du fichier doit correspondre au prompt généré");
        Assert.assertContains(fileContent, "relativité", "Le contenu doit contenir le sujet traité");
    }

    // Test d'export vers un fichier JSON (.json)
    public void testExportToFileJson() throws IOException {
        File tempJson = File.createTempFile("export_test_", ".json");
        tempJson.deleteOnExit();

        PromptProfile profile = Lemmatizer.analyser("Comparer PostgreSQL et MongoDB");
        CliArgs options = CliParser.parse(new String[]{"-o", tempJson.getAbsolutePath()});

        String prompt = MetaPromptEngine.genererPromptOptimise(profile, options);
        String json = MetaPromptEngine.genererExportJson(profile, prompt, options);
        Files.writeString(tempJson.toPath(), json);

        String readJson = Files.readString(tempJson.toPath());
        Assert.assertContains(readJson, "\"archetype\":", "Le fichier JSON doit contenir les métadonnées");
        Assert.assertContains(readJson, "PostgreSQL", "Le fichier JSON doit contenir le texte du prompt");
    }

    // Test de robustesse de la copie presse-papiers
    public void testClipboardCopyMethod() {
        // La méthode ne doit jamais lever d'exception non gérée
        boolean resultNull = CliClipboard.copierTexte(null);
        Assert.assertFalse(resultNull, "Copier null doit retourner false");

        boolean resultEmpty = CliClipboard.copierTexte("   ");
        Assert.assertFalse(resultEmpty, "Copier une chaîne vide doit retourner false");

        // Texte valide
        CliClipboard.copierTexte("Prompting tool test clipboard text");
    }
}
