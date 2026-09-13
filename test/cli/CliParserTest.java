package test.cli;

import cli.CliArgs;
import cli.CliParser;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// Suite de tests unitaires et stricts pour le parser d'arguments CLI et l'affichage d'aide
public class CliParserTest {

    // Test du flag d'aide (-h, --help) et validation de la bannière stylisée
    public void testHelpFlagEtBanniereStylisee() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-h"});
        Assert.assertTrue(argsCourt.isHelp(), "Le flag court -h doit activer isHelp");

        CliArgs argsLong = CliParser.parse(new String[]{"--help"});
        Assert.assertTrue(argsLong.isHelp(), "Le flag long --help doit activer isHelp");

        String help = CliParser.getHelpBanner();
        Assert.assertNotNull(help, "La bannière d'aide ne doit pas être null");

        // Validation stricte du format et de la bannière identique à Main/Menu
        Assert.assertContains(help, "*------------------------------------------*", "La bannière doit contenir la bordure supérieure");
        Assert.assertContains(help, "*  Prompting tool 4 a better work from AI  *", "La bannière doit contenir le titre exact du projet");
        Assert.assertContains(help, "*        MANUEL D'UTILISATION (CLI)        *", "La bannière doit mentionner le manuel CLI");

        // Validation de la présence de la description de tous les arguments
        Assert.assertContains(help, "-h, --help", "L'aide doit documenter -h, --help");
        Assert.assertContains(help, "-v, --version", "L'aide doit documenter -v, --version");
        Assert.assertContains(help, "-i, --instruction", "L'aide doit documenter -i, --instruction");
        Assert.assertContains(help, "-c, --code", "L'aide doit documenter -c, --code");
        Assert.assertContains(help, "-V, --verbose", "L'aide doit documenter -V, --verbose");
        Assert.assertContains(help, "-a, --agent", "L'aide doit documenter -a, --agent");
        Assert.assertContains(help, "-o, --output", "L'aide doit documenter -o, --output");
        Assert.assertContains(help, "-t, --template", "L'aide doit documenter -t, --template");
        Assert.assertContains(help, "-d, --domain", "L'aide doit documenter -d, --domain");
        Assert.assertContains(help, "-l, --language", "L'aide doit documenter -l, --language");
        Assert.assertContains(help, "-r, --raw", "L'aide doit documenter -r, --raw");
        Assert.assertContains(help, "-f, --file", "L'aide doit documenter -f, --file");
        Assert.assertContains(help, "-n, --dry-run", "L'aide doit documenter -n, --dry-run");
        Assert.assertContains(help, "-C, --clipboard", "L'aide doit documenter -C, --clipboard");
    }

    // Test du flag de version (-v, --version)
    public void testVersionFlag() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-v"});
        Assert.assertTrue(argsCourt.isVersion(), "Le flag -v doit activer isVersion");

        CliArgs argsLong = CliParser.parse(new String[]{"--version"});
        Assert.assertTrue(argsLong.isVersion(), "Le flag --version doit activer isVersion");

        String versionInfo = CliParser.getVersionInfo();
        Assert.assertContains(versionInfo, "1.0.0", "Les infos de version doivent contenir la version 1.0.0");
        Assert.assertContains(versionInfo, "Pierre Untersinger", "Les infos doivent mentionner l'auteur");
        Assert.assertContains(versionInfo, "@kaets0ner", "Les infos doivent mentionner le compte GitHub");
        Assert.assertNotContains(versionInfo, "@gmail", "Aucune adresse email ne doit figurer dans les métadonnées");
    }

    // Test du flag d'instruction (-i, --instruction) et des arguments positionnels
    public void testInstructionFlagEtPositionnels() {
        // Flag court avec espace
        CliArgs args1 = CliParser.parse(new String[]{"-i", "Créer une API REST avec Spring Boot"});
        Assert.assertTrue(args1.hasInstruction(), "hasInstruction doit être true");
        Assert.assertEquals("Créer une API REST avec Spring Boot", args1.instruction(), "L'instruction doit correspondre");

        // Flag long avec égal
        CliArgs args2 = CliParser.parse(new String[]{"--instruction=Refactoriser cette fonction"});
        Assert.assertEquals("Refactoriser cette fonction", args2.instruction(), "L'instruction doit être extraite");

        // Arguments positionnels sans flag
        CliArgs args3 = CliParser.parse(new String[]{"Comment", "marche", "le", "protocole", "Raft"});
        Assert.assertTrue(args3.hasInstruction(), "Les arguments positionnels doivent être convertis en instruction");
        Assert.assertEquals("Comment marche le protocole Raft", args3.instruction(), "Les mots positionnels doivent être joints");
    }

    // Test de l'injection de code (-c, --code) sous forme de snippet inline
    public void testCodeFlagInline() {
        String codeSnippet = "public static void main(String[] args) { System.out.println(\"Hello\"); }";
        CliArgs args1 = CliParser.parse(new String[]{"-c", codeSnippet});
        Assert.assertTrue(args1.hasCode(), "hasCode doit être true pour un snippet inline");
        Assert.assertEquals(codeSnippet, args1.code(), "Le snippet de code doit être préservé");

        CliArgs args2 = CliParser.parse(new String[]{"--code=const x = 42;"});
        Assert.assertEquals("const x = 42;", args2.code(), "Le snippet avec syntaxe --code= doit être extrait");
    }

    // Test de l'injection de code depuis un fichier source existant
    public void testCodeFlagDepuisFichier() throws IOException {
        File tempFile = File.createTempFile("test_code_", ".py");
        tempFile.deleteOnExit();

        String pyCode = "def calculate_sum(a, b):\n    return a + b";
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pyCode);
        }

        CliArgs args = CliParser.parse(new String[]{"-c", tempFile.getAbsolutePath()});
        Assert.assertTrue(args.hasCode(), "Le code doit être chargé depuis le fichier");
        Assert.assertEquals(pyCode, args.code(), "Le contenu du fichier source doit être lu fidèlement");
    }

    // Test du mode verbeux (-V, --verbose) et du rapport complet
    public void testVerboseFlagEtRapportComplet() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-V"});
        Assert.assertTrue(argsCourt.isVerbose(), "Le flag -V doit activer isVerbose");

        CliArgs argsLong = CliParser.parse(new String[]{"--verbose"});
        Assert.assertTrue(argsLong.isVerbose(), "Le flag --verbose doit activer isVerbose");

        // Génération d'un profil NLP réel pour tester le rapport verbeux
        PromptProfile profile = Lemmatizer.analyser("Déboguer ce script Python puis optimiser les requêtes SQL vers PostgreSQL");
        String rapport = CliParser.formatVerboseReport(profile);

        Assert.assertNotNull(rapport, "Le rapport verbeux ne doit pas être null");
        Assert.assertContains(rapport, "RAPPORT DÉTAILLÉ D'ANALYSE NLP (-V)", "Le rapport doit avoir le titre principal");
        Assert.assertContains(rapport, "[1. SANITIZATION & ÉLISIONS]", "Doit contenir la section Sanitization");
        Assert.assertContains(rapport, "[2. LEMMATISATION & CARACTÉRISTIQUES]", "Doit contenir la section Lemmatisation");
        Assert.assertContains(rapport, "[3. TECHNOLOGIES DÉTECTÉES]", "Doit contenir la section Technologies");
        Assert.assertContains(rapport, "[4. DOMAINE MÉTIER & PERSONA EXPERT]", "Doit contenir la section Domaine");
        Assert.assertContains(rapport, "[5. DÉCOMPOSITION DU FLUX DE PENSÉE]", "Doit contenir la section Décomposition");
        Assert.assertContains(rapport, "[6. DISTRIBUTION PROBABILISTE SOFTMAX]", "Doit contenir la distribution Softmax");
        Assert.assertContains(rapport, "[7. MÉTRIQUES DE TOKENS & ESTIMATION DES COÛTS]", "Doit contenir les coûts BPE");
        Assert.assertContains(rapport, "[8. DIAGNOSTIC DE QUALITÉ DU PROMPT]", "Doit contenir le diagnostic qualité");
    }

    // Test de la combinaison des 5 flags majeurs et génération du prompt complet
    public void testCombinaisonDesFlagsMajeurs() {
        String instruction = "Trouve la faille de sécurité dans cette fonction";
        String code = "String query = \"SELECT * FROM users WHERE id = \" + id;";

        CliArgs args = CliParser.parse(new String[]{
                "-i", instruction,
                "-c", code,
                "-V"
        });

        Assert.assertTrue(args.hasInstruction(), "Doit avoir une instruction");
        Assert.assertTrue(args.hasCode(), "Doit avoir du code");
        Assert.assertTrue(args.isVerbose(), "Doit être en mode verbeux");

        String fullPrompt = args.getFullPrompt();
        Assert.assertContains(fullPrompt, instruction, "Le prompt complet doit contenir l'instruction");
        Assert.assertContains(fullPrompt, "```\n" + code + "\n```", "Le prompt complet doit formater le code en bloc Markdown");
    }

    // Test des cas limites (arguments vides, nulls)
    public void testCasLimitesEtValeursParDefaut() {
        CliArgs argsVide = CliParser.parse(new String[]{});
        Assert.assertFalse(argsVide.isHelp(), "Par défaut isHelp est false");
        Assert.assertFalse(argsVide.isVersion(), "Par défaut isVersion est false");
        Assert.assertFalse(argsVide.isVerbose(), "Par défaut isVerbose est false");
        Assert.assertFalse(argsVide.hasInstruction(), "Par défaut hasInstruction est false");
        Assert.assertFalse(argsVide.hasCode(), "Par défaut hasCode est false");

        CliArgs argsNull = CliParser.parse(null);
        Assert.assertFalse(argsNull.isHelp(), "Null args donne un objet par défaut");
    }
}
