package test.llm;

import llm.*;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import test.framework.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Tests unitaires pour le moteur d'exécution LlmEngine et LocalLlmBackend
public class LlmEngineTest {

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

    public void testLlmEngineExecutionWithGrantedPermission() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes()));

        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(true); // Déjà autorisé

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner, new ProvisionerEspion());
        PromptProfile profile = Lemmatizer.analyser("Trouve la fuite mémoire dans cette classe Java public class Cache {}");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "auto", false);

        Assert.assertNotNull(result, "L'exécution doit aboutir");
        Assert.assertEquals(ModelType.QWEN_CODER, result.modelUsed(), "Routé vers Qwen Coder");

        String output = baos.toString();
        Assert.assertContains(output, "ROUTAGE DU MODELE EXPERT", "Bannière de routage");
        Assert.assertContains(output, "Statistiques", "Statistiques de performance");
    }

    public void testLlmEngineWithModelOverride() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes()));

        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(true);

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner, new ProvisionerEspion());
        PromptProfile profile = Lemmatizer.analyser("Bonjour");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "deepseek", false);

        Assert.assertNotNull(result, "Résultat non nul");
        Assert.assertEquals(ModelType.DEEPSEEK_REASONING, result.modelUsed(), "Modèle forcé DeepSeek");
    }

    public void testLlmEngineDeclinedPermission() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Utilisateur répond 3 (Refuser)
        Scanner scanner = new Scanner(new ByteArrayInputStream("3\n".getBytes()));

        LlmConfig config = new LlmConfig();
        config.setPermissionAccordee(false);

        LlmEngine engine = new LlmEngine(new LocalLlmBackend(), config, ps, scanner, new ProvisionerEspion());
        PromptProfile profile = Lemmatizer.analyser("Explique les trous noirs");

        LlmBackend.GenerationResult result = engine.execute("Prompt optimisé", profile, "auto", true);

        Assert.assertNull(result, "Si permission refusée, renvoie null");
        String output = baos.toString();
        Assert.assertContains(output, "ignore", "Mention d'annulation");
    }

    // --- Provisionnement du runtime natif (Issue #55) ---

    // Backend factice : evite de lancer un vrai binaire d'inference pendant les tests
    private static class BackendFactice implements LlmBackend {
        @Override
        public boolean isAvailable(ModelType model, String modelsDir) {
            return true;
        }

        @Override
        public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer tokenConsumer) {
            tokenConsumer.accept("ok");
            return new GenerationResult("reponse factice", 2, 10L, 200.0, model);
        }
    }

    // Enregistre les appels au lieu de telecharger reellement le runtime
    private static class ProvisionerEspion implements LlmEngine.RuntimeProvisioner {
        int appels = 0;
        String dernierRepertoire;

        @Override
        public boolean installer(String runtimeDir, PrintStream out) {
            appels++;
            dernierRepertoire = runtimeDir;
            return true;
        }
    }

    // Prepare un repertoire de modeles contenant deja le .gguf attendu (sans telechargement)
    private static Path creerModeleFactice(ModelType model) throws Exception {
        Path repertoire = Files.createTempDirectory("modeles_test");
        Files.writeString(repertoire.resolve(model.getNomFichier()), "FAUX_GGUF");
        return repertoire;
    }

    private static void supprimerRecursivement(Path racine) throws Exception {
        if (racine == null || !Files.exists(racine)) return;
        try (var flux = Files.walk(racine)) {
            flux.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    // Regression : un modele deja present sur le disque ne doit pas court-circuiter
    // l'installation du moteur d'inference natif.
    public void testRuntimeInstalleMemeQuandLeModeleEstDejaPresent() throws Exception {
        Path repertoireModeles = creerModeleFactice(ModelType.QWEN_CODER);
        Path repertoireRuntime = Files.createTempDirectory("runtime_test");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            LlmConfig config = new LlmConfig();
            config.setPermissionAccordee(true);
            config.setRepertoireModeles(repertoireModeles.toString());
            config.setRepertoireRuntime(repertoireRuntime.toString());

            ProvisionerEspion espion = new ProvisionerEspion();
            LlmEngine engine = new LlmEngine(new BackendFactice(), config, ps,
                    new Scanner(new ByteArrayInputStream(new byte[0])), espion);

            engine.execute("Prompt optimise", Lemmatizer.analyser("Corrige ce code Java"), "qwen", false);

            // Si la machine hote fournit deja llama-cli, aucune installation n'est attendue.
            if (!LocalLlmBackend.isRuntimeAvailable(repertoireRuntime.toString())) {
                Assert.assertEquals(1, espion.appels, "Le runtime doit etre installe meme si le modele est deja la");
                Assert.assertEquals(repertoireRuntime.toString(), espion.dernierRepertoire, "Repertoire runtime transmis");
            }
        } finally {
            supprimerRecursivement(repertoireModeles);
            supprimerRecursivement(repertoireRuntime);
        }
    }

    // Un runtime deja installe ne doit declencher aucun nouveau telechargement
    public void testRuntimeNonReinstalleSiDejaDisponible() throws Exception {
        Path repertoireModeles = creerModeleFactice(ModelType.QWEN_CODER);
        Path repertoireRuntime = Files.createTempDirectory("runtime_test");
        try {
            String nomBinaire = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "llama-cli.exe" : "llama-cli";
            Path binaire = repertoireRuntime.resolve(nomBinaire);
            Files.writeString(binaire, "FAUX_BINAIRE");
            binaire.toFile().setExecutable(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            LlmConfig config = new LlmConfig();
            config.setPermissionAccordee(true);
            config.setRepertoireModeles(repertoireModeles.toString());
            config.setRepertoireRuntime(repertoireRuntime.toString());

            ProvisionerEspion espion = new ProvisionerEspion();
            LlmEngine engine = new LlmEngine(new BackendFactice(), config, ps,
                    new Scanner(new ByteArrayInputStream(new byte[0])), espion);

            engine.execute("Prompt optimise", Lemmatizer.analyser("Corrige ce code Java"), "qwen", false);

            Assert.assertEquals(0, espion.appels, "Aucune reinstallation si le runtime est deja present");
        } finally {
            supprimerRecursivement(repertoireModeles);
            supprimerRecursivement(repertoireRuntime);
        }
    }

    // Sans autorisation et hors mode interactif, le mode degrade doit etre annonce explicitement
    public void testModeDegradeAnnonceExplicitementSansAutorisation() throws Exception {
        Path repertoireModeles = creerModeleFactice(ModelType.QWEN_CODER);
        Path repertoireRuntime = Files.createTempDirectory("runtime_test");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            LlmConfig config = new LlmConfig();
            config.setPermissionAccordee(false);
            config.setRepertoireModeles(repertoireModeles.toString());
            config.setRepertoireRuntime(repertoireRuntime.toString());

            ProvisionerEspion espion = new ProvisionerEspion();
            LlmEngine engine = new LlmEngine(new BackendFactice(), config, ps,
                    new Scanner(new ByteArrayInputStream(new byte[0])), espion);

            engine.execute("Prompt optimise", Lemmatizer.analyser("Corrige ce code Java"), "qwen", false);

            if (!LocalLlmBackend.isRuntimeAvailable(repertoireRuntime.toString())) {
                Assert.assertEquals(0, espion.appels, "Aucun telechargement sans autorisation");
                Assert.assertContains(baos.toString(), "mode degrade", "L'utilisateur doit etre averti de la simulation");
            }
        } finally {
            supprimerRecursivement(repertoireModeles);
            supprimerRecursivement(repertoireRuntime);
        }
    }
}
