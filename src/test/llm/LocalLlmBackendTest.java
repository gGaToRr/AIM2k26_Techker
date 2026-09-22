package test.llm;

import llm.LlmBackend;
import llm.LlmConfig;
import llm.LocalLlmBackend;
import llm.ModelType;
import test.framework.AfterEach;
import test.framework.Assert;
import test.framework.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Tests unitaires du moteur d'execution local (Issue #56).
//
// Aucun llama-cli n'est requis : les scenarios natifs utilisent un faux runner,
// un script shell qui reproduit le comportement attendu du binaire.
public class LocalLlmBackendTest {

    private final List<Path> aNettoyer = new ArrayList<>();

    @AfterEach
    public void nettoyer() throws Exception {
        for (Path p : aNettoyer) {
            if (Files.isDirectory(p)) {
                try (var flux = Files.walk(p)) {
                    flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
                }
            } else {
                Files.deleteIfExists(p);
            }
        }
        aNettoyer.clear();
    }

    private Path fauxRunner(String corpsScript) throws Exception {
        Path script = Files.createTempFile("faux_llama_cli", ".sh");
        Files.writeString(script, "#!/bin/sh\n" + corpsScript + "\n");
        script.toFile().setExecutable(true);
        aNettoyer.add(script);
        return script;
    }

    private Path fauxModele() throws Exception {
        Path modele = Files.createTempFile("faux_modele", ".gguf");
        Files.writeString(modele, "FAUX_GGUF");
        aNettoyer.add(modele);
        return modele;
    }

    // --- Bascule entre inference native et mode autonome ---

    @Test
    public void testBasculeNativeExigeLeMoteurEtLesPoids() throws Exception {
        Path modele = fauxModele();
        Path absent = Path.of("modele_absent_123456.gguf");

        Assert.assertTrue(LocalLlmBackend.doitUtiliserLeRuntimeNatif("/usr/bin/llama-cli", modele),
                "Moteur present + poids presents -> inference native");
        Assert.assertFalse(LocalLlmBackend.doitUtiliserLeRuntimeNatif(null, modele),
                "Sans moteur -> mode autonome");
        Assert.assertFalse(LocalLlmBackend.doitUtiliserLeRuntimeNatif("/usr/bin/llama-cli", absent),
                "Sans poids sur le disque -> mode autonome");
        Assert.assertFalse(LocalLlmBackend.doitUtiliserLeRuntimeNatif(null, null),
                "Ni l'un ni l'autre -> mode autonome");
    }

    // --- Construction de la ligne de commande ---

    @Test
    public void testCommandeContientLesParametresDInference() throws Exception {
        Path modele = fauxModele();
        LlmConfig config = new LlmConfig();

        List<String> commande = LocalLlmBackend.construireCommande("/opt/llama-cli", modele, "Explique X", config);

        Assert.assertEquals("/opt/llama-cli", commande.get(0), "Le binaire est en tete");
        Assert.assertTrue(commande.contains("-m"), "Drapeau du modele");
        Assert.assertTrue(commande.contains(modele.toAbsolutePath().toString()), "Chemin absolu du modele");
        Assert.assertTrue(commande.contains("-p"), "Drapeau du prompt");
        Assert.assertTrue(commande.contains("Explique X"), "Prompt transmis tel quel");
        Assert.assertTrue(commande.contains("--no-display-prompt"), "Le prompt ne doit pas etre reaffiche");
        Assert.assertTrue(commande.contains(String.valueOf(config.getMaxTokens())), "Plafond de tokens");
        Assert.assertTrue(commande.contains(String.valueOf(config.getTemperature())), "Temperature");
    }

    // Un prompt multi-ligne doit rester un unique argument, jamais decoupe par le shell
    @Test
    public void testPromptMultiLigneResteUnSeulArgument() throws Exception {
        Path modele = fauxModele();
        String prompt = "ligne une\nligne deux\nligne trois";

        List<String> commande = LocalLlmBackend.construireCommande("/opt/llama-cli", modele, prompt, new LlmConfig());

        Assert.assertTrue(commande.contains(prompt), "Le prompt entier forme un seul argument");
        Assert.assertEquals(10, commande.size(), "Aucun argument supplementaire cree par les sauts de ligne");
    }

    // --- Parsing du flux de sortie ---

    @Test
    public void testParsingDuFluxDeSortie() throws Exception {
        Path runner = fauxRunner("printf 'Bonjour le monde.'");
        Path modele = fauxModele();
        List<String> recus = new ArrayList<>();

        LlmBackend.GenerationResult resultat = new LocalLlmBackend().runNativeInference(
                runner.toString(), modele, ModelType.QWEN_CODER, "peu importe",
                new LlmConfig(), recus::add, System.currentTimeMillis());

        Assert.assertEquals("Bonjour le monde.", resultat.fullText(), "Texte complet reconstitue");
        Assert.assertTrue(resultat.totalTokens() > 0, "Tokens comptabilises");
        Assert.assertEquals(String.join("", recus), "Bonjour le monde.",
                "Les morceaux streames, concatenes, redonnent la sortie exacte");
        Assert.assertEquals(ModelType.QWEN_CODER, resultat.modelUsed(), "Modele reporte");
    }

    @Test
    public void testFluxVideProduitUnResultatVideSansEchec() throws Exception {
        Path runner = fauxRunner("exit 0");
        Path modele = fauxModele();

        LlmBackend.GenerationResult resultat = new LocalLlmBackend().runNativeInference(
                runner.toString(), modele, ModelType.SMOLLM_FAST, "peu importe",
                new LlmConfig(), null, System.currentTimeMillis());

        Assert.assertEquals("", resultat.fullText(), "Sortie vide");
        Assert.assertEquals(0, resultat.totalTokens(), "Aucun token compte");
    }

    // --- Gestion des erreurs de process ---

    @Test
    public void testCodeRetourNonNulRemonteUneErreur() throws Exception {
        Path runner = fauxRunner("echo 'error: failed to load model'\nexit 1");
        Path modele = fauxModele();

        try {
            new LocalLlmBackend().runNativeInference(runner.toString(), modele, ModelType.QWEN_CODER,
                    "peu importe", new LlmConfig(), null, System.currentTimeMillis());
            Assert.assertTrue(false, "Un code retour non nul doit lever une exception");
        } catch (IllegalStateException attendue) {
            Assert.assertContains(attendue.getMessage(), "code 1", "Le code retour est rapporte");
            Assert.assertContains(attendue.getMessage(), "failed to load model",
                    "La sortie de diagnostic est jointe");
        }
    }

    // Le message d'erreur de llama-cli ne doit jamais etre presente comme une reponse du modele
    @Test
    public void testSortieDErreurNestPasPresenteeCommeUneReponse() throws Exception {
        Path runner = fauxRunner("echo 'segmentation fault'\nexit 139");
        Path modele = fauxModele();

        try {
            LlmBackend.GenerationResult resultat = new LocalLlmBackend().runNativeInference(
                    runner.toString(), modele, ModelType.QWEN_CODER, "peu importe",
                    new LlmConfig(), null, System.currentTimeMillis());
            Assert.assertTrue(false, "Un crash ne doit pas produire un GenerationResult : " + resultat.fullText());
        } catch (IllegalStateException attendue) {
            Assert.assertContains(attendue.getMessage(), "139", "Le code de crash est rapporte");
        }
    }

    @Test
    public void testProcessusBloqueEstInterrompu() throws Exception {
        Path runner = fauxRunner("printf 'debut '\nsleep 30");
        Path modele = fauxModele();

        long avant = System.currentTimeMillis();
        try {
            LocalLlmBackend.delaiMaxInferenceSecondes = 1L;
            new LocalLlmBackend().runNativeInference(runner.toString(), modele, ModelType.QWEN_CODER,
                    "peu importe", new LlmConfig(), null, System.currentTimeMillis());
            Assert.assertTrue(false, "Un processus bloque doit etre interrompu");
        } catch (IllegalStateException attendue) {
            Assert.assertContains(attendue.getMessage(), "rendu la main", "Cause explicitee");
        } finally {
            LocalLlmBackend.delaiMaxInferenceSecondes = 600L;
        }

        Assert.assertTrue(System.currentTimeMillis() - avant < 25_000,
                "L'interruption ne doit pas attendre la fin naturelle du processus");
    }

    // --- Mode autonome ---

    @Test
    public void testModeAutonomeStreameEtIdentifieLeModele() throws Exception {
        List<String> recus = new ArrayList<>();
        LlmConfig config = new LlmConfig();

        LlmBackend.GenerationResult resultat = new LocalLlmBackend()
                .generate(ModelType.GEMMA_GENERAL, "Explique les marees", config, recus::add);

        Assert.assertEquals(ModelType.GEMMA_GENERAL, resultat.modelUsed(), "Modele reporte");
        Assert.assertTrue(recus.size() > 0, "Des tokens sont streames");
        Assert.assertStrictPositive(resultat.tokensPerSecond(), "Debit calcule");
        Assert.assertContains(resultat.fullText(), ModelType.GEMMA_GENERAL.getNomAffiche(),
                "Le mode autonome s'annonce clairement comme tel");
    }

    @Test
    public void testIsAvailableSuitLaPresenceDuFichierDeModele() throws Exception {
        Path dossier = Files.createTempDirectory("modeles_dispo");
        aNettoyer.add(dossier);
        LocalLlmBackend backend = new LocalLlmBackend();

        Assert.assertFalse(backend.isAvailable(ModelType.QWEN_CODER, dossier.toString()),
                "Absent au depart");

        Files.writeString(dossier.resolve(ModelType.QWEN_CODER.getNomFichier()), "FAUX");
        Assert.assertTrue(backend.isAvailable(ModelType.QWEN_CODER, dossier.toString()),
                "Present une fois le fichier depose");
    }
}
