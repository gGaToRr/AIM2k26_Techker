package test.cli;

import cli.CliArgs;
import cli.CliParser;
import gen.MetaPromptEngine;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

// Tests de robustesse, tolérance aux fautes et cas limites pour les arguments CLI (Issue #34)
public class CliRobustnessAndEdgeCasesTest {

    // Test de gestion d'un fichier source inexistant pour le flag -f/--file
    public void testNonExistentSourceFileHandling() {
        CliArgs args = CliParser.parse(new String[]{"-f", "dossier_inexistant/fichier_inexistant_123.txt"});
        Assert.assertEquals("dossier_inexistant/fichier_inexistant_123.txt", args.filePath(), "Le chemin doit être enregistré");
        Assert.assertEquals("", args.instruction(), "Le contenu de l'instruction doit être vide sans lever d'exception");
    }

    // Test de gestion d'un code snippet qui ressemble à un chemin mais n'existe pas
    public void testNonExistentCodeFileTreatedAsInlineString() {
        String codeInput = "path/to/virtual/class.java { void run() {} }";
        CliArgs args = CliParser.parse(new String[]{"-c", codeInput});
        Assert.assertTrue(args.hasCode(), "Le code doit être renseigné");
        Assert.assertEquals(codeInput, args.code(), "Le code doit être conservé tel quel");
    }

    // Test de flags avec valeurs manquantes en fin de tableau d'arguments (pas de ArrayIndexOutOfBoundsException)
    public void testMissingArgumentValuesAtEndOfArgs() {
        // Flag -i sans valeur
        CliArgs args1 = CliParser.parse(new String[]{"-i"});
        Assert.assertFalse(args1.hasInstruction(), "Instruction doit être absente sans crash");

        // Flag -c sans valeur
        CliArgs args2 = CliParser.parse(new String[]{"-c"});
        Assert.assertFalse(args2.hasCode(), "Code doit être absent sans crash");

        // Flag -a sans valeur
        CliArgs args3 = CliParser.parse(new String[]{"-a"});
        Assert.assertFalse(args3.hasAgent(), "Agent doit être absent sans crash");

        // Flag -t sans valeur
        CliArgs args4 = CliParser.parse(new String[]{"-t"});
        Assert.assertFalse(args4.hasTemplate(), "Template doit être absent sans crash");

        // Flag -o sans valeur
        CliArgs args5 = CliParser.parse(new String[]{"-o"});
        Assert.assertFalse(args5.hasOutput(), "Output doit être absent sans crash");
    }

    // Test de la préservation intégrale du code avec caractères spéciaux et balises XML
    public void testSpecialCharactersAndCodeBlockIntegrity() {
        String complexCode = """
                <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>demo & test</artifactId>
                    <version>"1.0.0"</version>
                </dependency>
                """;

        CliArgs args = CliParser.parse(new String[]{
                "-i", "Valider ce fragment XML",
                "-c", complexCode
        });

        String fullPrompt = args.getFullPrompt();
        Assert.assertContains(fullPrompt, "<dependency>", "Doit préserver la balise <dependency>");
        Assert.assertContains(fullPrompt, "demo & test", "Doit préserver les esperluettes &");
        Assert.assertContains(fullPrompt, "\"1.0.0\"", "Doit préserver les guillemets");

        // Rendu complet
        PromptProfile profile = Lemmatizer.analyser(fullPrompt);
        String prompt = MetaPromptEngine.genererPromptOptimise(profile, args);
        Assert.assertContains(prompt, "<dependency>", "Le prompt généré doit préserver les balises XML du code");
    }

    // Test d'arguments contenant des espaces multiples et tabulations
    public void testMultipleWhitespacesInArguments() {
        CliArgs args = CliParser.parse(new String[]{
                "  -i  ", "   Instruction   avec   espaces   multiples   ",
                "  -V  "
        });

        Assert.assertTrue(args.hasInstruction(), "L'instruction doit être reconnue");
        Assert.assertEquals("Instruction   avec   espaces   multiples", args.instruction(), "Les espaces superflus aux extrémités doivent être nettoyés");
        Assert.assertTrue(args.isVerbose(), "Le mode verbeux doit être activé malgré les espaces dans le flag");
    }
}
