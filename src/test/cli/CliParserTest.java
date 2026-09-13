package test.cli;

import cli.CliArgs;
import cli.CliParser;
import gen.MetaPromptEngine;
import gen.TemplateLoader;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Suite de tests unitaires et d'intégration stricts pour l'ensemble des arguments CLI
public class CliParserTest {

    // 1. Test du flag d'aide (-h, --help) et validation de la bannière stylisée
    public void testHelpFlagEtBanniereStylisee() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-h"});
        Assert.assertTrue(argsCourt.isHelp(), "Le flag court -h doit activer isHelp");

        CliArgs argsLong = CliParser.parse(new String[]{"--help"});
        Assert.assertTrue(argsLong.isHelp(), "Le flag long --help doit activer isHelp");

        String help = CliParser.getHelpBanner();
        Assert.assertNotNull(help, "La bannière d'aide ne doit pas être null");

        // Validation stricte de la bannière identique à Main/Menu
        Assert.assertContains(help, "*------------------------------------------*", "La bannière doit contenir la bordure supérieure");
        Assert.assertContains(help, "*  Prompting tool 4 a better work from AI  *", "La bannière doit contenir le titre exact du projet");
        Assert.assertContains(help, "*        MANUEL D'UTILISATION (CLI)        *", "La bannière doit mentionner le manuel CLI");

        // Validation de la présence de la description des arguments
        Assert.assertContains(help, "-h, --help", "L'aide doit documenter -h, --help");
        Assert.assertContains(help, "-v, --version", "L'aide doit documenter -v, --version");
        Assert.assertContains(help, "-i, --instruction", "L'aide doit documenter -i, --instruction");
        Assert.assertContains(help, "-c, --code", "L'aide doit documenter -c, --code");
        Assert.assertContains(help, "-V, --verbose", "L'aide doit documenter -V, --verbose");
        Assert.assertContains(help, "-e, --exec", "L'aide doit documenter -e, --exec");
        Assert.assertContains(help, "-m, --model", "L'aide doit documenter -m, --model");
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

    // 2. Test du flag de version (-v, --version)
    public void testVersionFlag() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-v"});
        Assert.assertTrue(argsCourt.isVersion(), "Le flag -v doit activer isVersion");

        CliArgs argsLong = CliParser.parse(new String[]{"--version"});
        Assert.assertTrue(argsLong.isVersion(), "Le flag --version doit activer isVersion");

        String versionInfo = CliParser.getVersionInfo();
        Assert.assertContains(versionInfo, "1.1.0", "Les infos de version doivent contenir la version 1.1.0");
        Assert.assertContains(versionInfo, "Pierre Untersinger", "Les infos doivent mentionner l'auteur");
        Assert.assertContains(versionInfo, "@kaets0ner", "Les infos doivent mentionner le compte GitHub");
        Assert.assertNotContains(versionInfo, "@gmail", "Aucune adresse email ne doit figurer dans les métadonnées");
    }

    // 3. Test du flag d'instruction (-i, --instruction) et des arguments positionnels
    public void testInstructionFlagEtPositionnels() {
        CliArgs args1 = CliParser.parse(new String[]{"-i", "Créer une API REST avec Spring Boot"});
        Assert.assertTrue(args1.hasInstruction(), "hasInstruction doit être true");
        Assert.assertEquals("Créer une API REST avec Spring Boot", args1.instruction(), "L'instruction doit correspondre");

        CliArgs args2 = CliParser.parse(new String[]{"--instruction=Refactoriser cette fonction"});
        Assert.assertEquals("Refactoriser cette fonction", args2.instruction(), "L'instruction doit être extraite");

        CliArgs args3 = CliParser.parse(new String[]{"Comment", "marche", "le", "protocole", "Raft"});
        Assert.assertTrue(args3.hasInstruction(), "Les arguments positionnels doivent être convertis en instruction");
        Assert.assertEquals("Comment marche le protocole Raft", args3.instruction(), "Les mots positionnels doivent être joints");
    }

    // 4. Test de l'injection de code (-c, --code)
    public void testCodeFlagInlineEtFichier() throws IOException {
        String codeSnippet = "public static void main(String[] args) { System.out.println(\"Hello\"); }";
        CliArgs args1 = CliParser.parse(new String[]{"-c", codeSnippet});
        Assert.assertTrue(args1.hasCode(), "hasCode doit être true");
        Assert.assertEquals(codeSnippet, args1.code(), "Le snippet de code doit être préservé");

        File tempFile = File.createTempFile("test_code_", ".py");
        tempFile.deleteOnExit();
        String pyCode = "def sum(a, b): return a + b";
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(pyCode);
        }

        CliArgs args2 = CliParser.parse(new String[]{"-c", tempFile.getAbsolutePath()});
        Assert.assertEquals(pyCode, args2.code(), "Le fichier doit être lu et injecté en tant que code");
    }

    // 5. Test du mode verbeux (-V, --verbose)
    public void testVerboseFlagEtRapportComplet() {
        CliArgs args = CliParser.parse(new String[]{"-V"});
        Assert.assertTrue(args.isVerbose(), "Le flag -V doit activer isVerbose");

        PromptProfile profile = Lemmatizer.analyser("Déboguer ce script Python puis optimiser les requêtes SQL vers PostgreSQL");
        String rapport = CliParser.formatVerboseReport(profile);

        Assert.assertNotNull(rapport, "Le rapport verbeux ne doit pas être null");
        Assert.assertContains(rapport, "RAPPORT DÉTAILLÉ D'ANALYSE NLP (-V)", "Doit contenir le titre");
        Assert.assertContains(rapport, "[1. SANITIZATION & ÉLISIONS]", "Doit contenir Sanitization");
        Assert.assertContains(rapport, "[2. LEMMATISATION & CARACTÉRISTIQUES]", "Doit contenir Lemmatisation");
        Assert.assertContains(rapport, "[3. TECHNOLOGIES DÉTECTÉES]", "Doit contenir Technologies");
        Assert.assertContains(rapport, "[4. DOMAINE MÉTIER & PERSONA EXPERT]", "Doit contenir Domaine");
        Assert.assertContains(rapport, "[5. DÉCOMPOSITION DU FLUX DE PENSÉE]", "Doit contenir Décomposition");
        Assert.assertContains(rapport, "[6. DISTRIBUTION PROBABILISTE SOFTMAX]", "Doit contenir Softmax");
        Assert.assertContains(rapport, "[7. MÉTRIQUES DE TOKENS & ESTIMATION DES COÛTS]", "Doit contenir BPE");
        Assert.assertContains(rapport, "[8. DIAGNOSTIC DE QUALITÉ DU PROMPT]", "Doit contenir Qualité");
    }

    // 6. Test du ciblage d'agent / modèle IA (-a, --agent) (Issue #20)
    public void testAgentFlagEtAdaptationPrompt() {
        CliArgs argsClaude = CliParser.parse(new String[]{"-a", "claude"});
        Assert.assertTrue(argsClaude.hasAgent(), "hasAgent doit être true");
        Assert.assertEquals("claude", argsClaude.agent(), "L'agent doit être claude");

        PromptProfile profile = Lemmatizer.analyser("Explique la récursion en informatique");
        String promptClaude = MetaPromptEngine.genererPromptOptimise(profile, argsClaude);
        Assert.assertContains(promptClaude, "<claude_system_prompt>", "Le prompt Claude doit contenir la balise système Claude");
        Assert.assertContains(promptClaude, "<thinking>", "Le prompt Claude doit contenir les directives de pensée");

        CliArgs argsDeepSeek = CliParser.parse(new String[]{"--agent=deepseek"});
        String promptDeepSeek = MetaPromptEngine.genererPromptOptimise(profile, argsDeepSeek);
        Assert.assertContains(promptDeepSeek, "DeepSeek-V3", "Le prompt DeepSeek doit contenir l'en-tête DeepSeek");
        Assert.assertContains(promptDeepSeek, "<think>", "Le prompt DeepSeek doit contenir les directives <think>");

        CliArgs argsGpt = CliParser.parse(new String[]{"-a", "gpt-4o"});
        String promptGpt = MetaPromptEngine.genererPromptOptimise(profile, argsGpt);
        Assert.assertContains(promptGpt, "OpenAI GPT-4o", "Le prompt GPT doit contenir la directive GPT-4o");
    }

    // 7. Test du formatage et de l'export de sortie (-o, --output) (Issue #21)
    public void testOutputFlagJsonEtExportFichier() throws IOException {
        CliArgs argsJson = CliParser.parse(new String[]{"-o", "json"});
        Assert.assertTrue(argsJson.hasOutput(), "hasOutput doit être true");
        Assert.assertEquals("json", argsJson.output(), "Le format doit être json");

        PromptProfile profile = Lemmatizer.analyser("Rédiger un article sur l'intelligence artificielle");
        String prompt = MetaPromptEngine.genererPromptOptimise(profile, argsJson);
        String json = MetaPromptEngine.genererExportJson(profile, prompt, argsJson);

        Assert.assertContains(json, "\"project\": \"Prompting tool 4 a better work from AI", "Le JSON doit contenir le nom du projet");
        Assert.assertContains(json, "\"prompt_profile\": {", "Le JSON doit contenir l'objet prompt_profile");
        Assert.assertContains(json, "\"meta_prompt\":", "Le JSON doit contenir le super prompt");

        // Test export fichier
        File tempOut = File.createTempFile("prompt_export_", ".md");
        tempOut.deleteOnExit();
        CliArgs argsFile = CliParser.parse(new String[]{"-o", tempOut.getAbsolutePath()});
        Assert.assertEquals(tempOut.getAbsolutePath(), argsFile.output(), "Le chemin du fichier doit être conservé");
    }

    // 8. Test du forçage de template (-t, --template) (Issue #22)
    public void testTemplateOverrideFlag() {
        CliArgs args = CliParser.parse(new String[]{"-t", "recette_culinaire"});
        Assert.assertTrue(args.hasTemplate(), "hasTemplate doit être true");
        Assert.assertEquals("recette_culinaire", args.template(), "Le template forcé doit être recette_culinaire");

        // Même sur un prompt tech, le template forcé recette_culinaire doit s'appliquer
        PromptProfile profile = Lemmatizer.analyser("Optimiser un serveur Java");
        String prompt = MetaPromptEngine.genererPromptOptimise(profile, args);
        Assert.assertContains(prompt, "directives_culinaires", "Le template recette forcé doit contenir la section directives_culinaires");
        Assert.assertContains(prompt, "recettes de cuisine", "Le template recette forcé doit contenir les consignes culinaires");
    }

    // 9. Test du forçage de domaine (-d, --domain) (Issue #23)
    public void testDomainOverrideFlag() {
        CliArgs args = CliParser.parse(new String[]{"-d", "Cybersecurity & Pentesting"});
        Assert.assertTrue(args.hasDomain(), "hasDomain doit être true");
        Assert.assertEquals("Cybersecurity & Pentesting", args.domain(), "Le domaine forcé doit correspondre");

        PromptProfile profile = Lemmatizer.analyser("Analyser ce composant");
        String prompt = MetaPromptEngine.genererPromptOptimise(profile, args);
        Assert.assertContains(prompt, "Cybersecurity & Pentesting", "Le persona forcé de cybersécurité doit être injecté");
    }

    // 10. Test de la spécification de langue (-l, --language) (Issue #24)
    public void testLanguageOverrideFlag() {
        CliArgs args = CliParser.parse(new String[]{"-l", "en"});
        Assert.assertTrue(args.hasLanguage(), "hasLanguage doit être true");
        Assert.assertEquals("en", args.language(), "La langue doit être en");

        PromptProfile profile = Lemmatizer.analyser("Bonjour, comment programmer en C++");
        String prompt = MetaPromptEngine.genererPromptOptimise(profile, args);
        Assert.assertContains(prompt, "English", "La langue cible English doit être injectée dans le contexte");
    }

    // 11. Test du mode sortie brute (-r, --raw) (Issue #25)
    public void testRawModeFlag() {
        CliArgs args = CliParser.parse(new String[]{"-r"});
        Assert.assertTrue(args.isRaw(), "isRaw doit être true avec le flag -r");

        CliArgs argsLong = CliParser.parse(new String[]{"--raw"});
        Assert.assertTrue(argsLong.isRaw(), "isRaw doit être true avec le flag --raw");
    }

    // 12. Test du chargement de prompt depuis un fichier (-f, --file) (Issue #26)
    public void testFileLoadingFlag() throws IOException {
        File promptFile = File.createTempFile("input_prompt_", ".txt");
        promptFile.deleteOnExit();
        String fileContent = "Explique l'algorithme de consensus Raft de manière détaillée.";
        Files.writeString(promptFile.toPath(), fileContent);

        CliArgs args = CliParser.parse(new String[]{"-f", promptFile.getAbsolutePath()});
        Assert.assertTrue(args.hasInstruction(), "L'instruction doit être chargée depuis le fichier");
        Assert.assertEquals(fileContent, args.instruction(), "Le contenu de l'instruction doit correspondre");
    }

    // 13. Test du mode analyse seule (-n, --dry-run) (Issue #27)
    public void testDryRunFlag() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-n"});
        Assert.assertTrue(argsCourt.isDryRun(), "isDryRun doit être true avec -n");

        CliArgs argsLong = CliParser.parse(new String[]{"--dry-run"});
        Assert.assertTrue(argsLong.isDryRun(), "isDryRun doit être true avec --dry-run");
    }

    // 14. Test de la copie presse-papiers (-C, --clipboard) (Issue #28)
    public void testClipboardFlag() {
        CliArgs argsCourt = CliParser.parse(new String[]{"-C"});
        Assert.assertTrue(argsCourt.isClipboard(), "isClipboard doit être true avec -C");

        CliArgs argsLong = CliParser.parse(new String[]{"--clipboard"});
        Assert.assertTrue(argsLong.isClipboard(), "isClipboard doit être true avec --clipboard");
    }
}
