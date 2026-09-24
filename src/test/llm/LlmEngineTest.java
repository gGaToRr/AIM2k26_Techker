package test.llm;

import llm.*;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.AfterEach;
import test.framework.Assert;
import test.framework.BeforeEach;
import test.framework.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Tests unitaires pour le moteur d'exécution LlmEngine et LocalLlmBackend
public class LlmEngineTest {

    // Modeles factices dans un dossier temporaire : sans eux, une permission accordee
    // declenche le telechargement reel (~1 Go par modele) dans models/ du projet.
    private Path repertoireModeles;

    @BeforeEach
    public void preparerModelesFactices() throws Exception {
        repertoireModeles = Files.createTempDirectory("engine_test_models_");
        for (ModelType model : ModelType.values()) {
            Files.createFile(repertoireModeles.resolve(model.getNomFichier()));
        }
    }

    @AfterEach
    public void supprimerModelesFactices() throws Exception {
        try (var chemins = Files.walk(repertoireModeles)) {
            chemins.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private LlmConfig configIsolee(boolean permission) {
        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(permission);
        config.setRepertoireModeles(repertoireModeles.toString());
        return config;
    }

    @Test
    public void testLocalBackendEmbeddedInference() throws Exception {
        LocalLlmBackend backend = new LocalLlmBackend();
        LlmConfig config = new LlmConfig();
        List<String> streamedTokens = new ArrayList<>();

        LlmBackend.GenerationResult result = backend.generate(
                ModelType.QWEN_CODER,
                "Écris une fonction de tri en Java",
                config,
                streamedTokens::add
        );

        Assert.assertNotNull(result, "Résultat non nul");
        Assert.assertEquals(ModelType.QWEN_CODER, result.modelUsed(), "Modèle utilisé");
        Assert.assertTrue(result.totalTokens() > 0, "Tokens comptabilisés");
        Assert.assertTrue(result.tokensPerSecond() > 0, "Débit calculé");
        Assert.assertTrue(streamedTokens.size() > 0, "Tokens streamés reçus");
        Assert.assertContains(result.fullText(), "Qwen 2.5 Coder", "Contenu retourné");
    }

    @Test
    public void testLlmEngineExecutionWithGrantedPermission() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes()));

        LlmConfig config = configIsolee(true); // Déjà autorisé

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner);
        PromptProfile profile = Lemmatizer.analyser("Trouve la fuite mémoire dans cette classe Java public class Cache {}");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "auto", false);

        Assert.assertNotNull(result, "L'exécution doit aboutir");
        Assert.assertEquals(ModelType.QWEN_CODER, result.modelUsed(), "Routé vers Qwen Coder");

        String output = baos.toString();
        Assert.assertContains(output, "ROUTAGE DU MODELE EXPERT", "Bannière de routage");
        Assert.assertContains(output, "Statistiques", "Statistiques de performance");
    }

    @Test
    public void testLlmEngineWithModelOverride() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes()));

        LlmConfig config = configIsolee(true);

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner);
        PromptProfile profile = Lemmatizer.analyser("Bonjour");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "deepseek", false);

        Assert.assertNotNull(result, "Résultat non nul");
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, result.modelUsed(), "Modèle forcé DeepSeek");
    }

    @Test
    public void testLlmEngineDeclinedPermission() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Utilisateur répond 3 (Refuser)
        Scanner scanner = new Scanner(new ByteArrayInputStream("3\n".getBytes()));

        // Aucun modele installe : sinon le repli sur un modele present court-circuite l'onboarding
        LlmConfig config = configIsolee(false);
        for (ModelType model : ModelType.values()) {
            Files.deleteIfExists(repertoireModeles.resolve(model.getNomFichier()));
        }

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner);
        PromptProfile profile = Lemmatizer.analyser("Explique les trous noirs");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "auto", true);

        Assert.assertNull(result, "Si permission refusée, renvoie null");
        String output = baos.toString();
        Assert.assertContains(output, "ignore", "Mention d'annulation");
    }
}
