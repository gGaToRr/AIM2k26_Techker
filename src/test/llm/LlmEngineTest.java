package test.llm;

import llm.*;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;
import test.framework.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Tests unitaires pour le moteur d'exécution LlmEngine et LocalLlmBackend
public class LlmEngineTest {

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

        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(true); // Déjà autorisé

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

        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(true);

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner);
        PromptProfile profile = Lemmatizer.analyser("Bonjour");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "deepseek", false);

        Assert.assertNotNull(result, "Résultat non nul");
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, result.modelUsed(), "Modèle forcé DeepSeek");
    }

    @Test
    public void testLlmEngineDeclinedPermission() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Utilisateur répond 3 (Refuser)
        Scanner scanner = new Scanner(new ByteArrayInputStream("3\n".getBytes()));

        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(false);

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner);
        PromptProfile profile = Lemmatizer.analyser("Explique les trous noirs");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "auto", true);

        Assert.assertNull(result, "Si permission refusée, renvoie null");
        String output = baos.toString();
        Assert.assertContains(output, "ignore", "Mention d'annulation");
    }
}
