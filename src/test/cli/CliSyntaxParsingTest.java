package test.cli;

import cli.CliArgs;
import cli.CliParser;
import test.framework.Assert;
import test.framework.Test;

// Tests unitaires exhaustifs pour toutes les syntaxes de parsing d'arguments CLI (Issue #31)
public class CliSyntaxParsingTest {

    // Test de tous les flags booléens en versions courtes et longues
    @Test
    public void testAllBooleanFlagsShortAndLong() {
        // Aide
        Assert.assertTrue(CliParser.parse(new String[]{"-h"}).isHelp(), "Flag -h");
        Assert.assertTrue(CliParser.parse(new String[]{"--help"}).isHelp(), "Flag --help");

        // Version
        Assert.assertTrue(CliParser.parse(new String[]{"-v"}).isVersion(), "Flag -v");
        Assert.assertTrue(CliParser.parse(new String[]{"--version"}).isVersion(), "Flag --version");

        // Verbose
        Assert.assertTrue(CliParser.parse(new String[]{"-V"}).isVerbose(), "Flag -V");
        Assert.assertTrue(CliParser.parse(new String[]{"--verbose"}).isVerbose(), "Flag --verbose");

        // Raw
        Assert.assertTrue(CliParser.parse(new String[]{"-r"}).isRaw(), "Flag -r");
        Assert.assertTrue(CliParser.parse(new String[]{"--raw"}).isRaw(), "Flag --raw");

        // Dry-Run
        Assert.assertTrue(CliParser.parse(new String[]{"-n"}).isDryRun(), "Flag -n");
        Assert.assertTrue(CliParser.parse(new String[]{"--dry-run"}).isDryRun(), "Flag --dry-run");

        // Clipboard
        Assert.assertTrue(CliParser.parse(new String[]{"-C"}).isClipboard(), "Flag -C");
        Assert.assertTrue(CliParser.parse(new String[]{"--clipboard"}).isClipboard(), "Flag --clipboard");
    }

    // Test de la syntaxe --flag=valeur pour tous les arguments paramétrés
    @Test
    public void testFlagsWithLongEqualSignSyntax() {
        String[] args = {
                "--instruction=Analyse ce bug critique",
                "--code=const x = null;",
                "--agent=deepseek",
                "--output=json",
                "--template=root_cause_debug",
                "--domain=cybersecurity",
                "--language=en"
        };

        CliArgs parsed = CliParser.parse(args);

        Assert.assertEquals("Analyse ce bug critique", parsed.instruction(), "Extraction --instruction=");
        Assert.assertEquals("const x = null;", parsed.code(), "Extraction --code=");
        Assert.assertEquals("deepseek", parsed.agent(), "Extraction --agent=");
        Assert.assertEquals("json", parsed.output(), "Extraction --output=");
        Assert.assertEquals("root_cause_debug", parsed.template(), "Extraction --template=");
        Assert.assertEquals("cybersecurity", parsed.domain(), "Extraction --domain=");
        Assert.assertEquals("en", parsed.language(), "Extraction --language=");
    }

    // Test de la syntaxe -f=valeur (flags courts avec signe égal)
    @Test
    public void testFlagsWithShortEqualSignSyntax() {
        String[] args = {
                "-i=Explique le consensus",
                "-c=int x = 42;",
                "-a=claude",
                "-o=result.md",
                "-t=feynman_learning",
                "-d=science",
                "-l=fr"
        };

        CliArgs parsed = CliParser.parse(args);

        Assert.assertEquals("Explique le consensus", parsed.instruction(), "Extraction -i=");
        Assert.assertEquals("int x = 42;", parsed.code(), "Extraction -c=");
        Assert.assertEquals("claude", parsed.agent(), "Extraction -a=");
        Assert.assertEquals("result.md", parsed.output(), "Extraction -o=");
        Assert.assertEquals("feynman_learning", parsed.template(), "Extraction -t=");
        Assert.assertEquals("science", parsed.domain(), "Extraction -d=");
        Assert.assertEquals("fr", parsed.language(), "Extraction -l=");
    }

    // Test de l'ordre arbitraire des arguments
    @Test
    public void testArbitraryArgumentOrder() {
        String[] args = {
                "-V",
                "-a", "gpt",
                "-i", "Génère un microservice",
                "-r",
                "-t", "code_generation",
                "-d", "software_engineering",
                "-c", "class App {}",
                "-C"
        };

        CliArgs parsed = CliParser.parse(args);

        Assert.assertTrue(parsed.isVerbose(), "Verbose doit être activé");
        Assert.assertTrue(parsed.isRaw(), "Raw doit être activé");
        Assert.assertTrue(parsed.isClipboard(), "Clipboard doit être activé");
        Assert.assertEquals("gpt", parsed.agent(), "Agent gpt");
        Assert.assertEquals("Génère un microservice", parsed.instruction(), "Instruction");
        Assert.assertEquals("code_generation", parsed.template(), "Template");
        Assert.assertEquals("software_engineering", parsed.domain(), "Domaine");
        Assert.assertEquals("class App {}", parsed.code(), "Code");
    }

    // Test de l'extraction des arguments positionnels comme instruction par défaut
    @Test
    public void testPositionalArgumentsExtraction() {
        String[] args = {"Optimise", "cette", "requête", "SQL", "vers", "PostgreSQL"};
        CliArgs parsed = CliParser.parse(args);

        Assert.assertTrue(parsed.hasInstruction(), "Les arguments positionnels doivent former l'instruction");
        Assert.assertEquals("Optimise cette requête SQL vers PostgreSQL", parsed.instruction(), "Les mots doivent être reliés par des espaces");
    }
}
