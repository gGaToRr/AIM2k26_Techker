package test.llm;

import llm.LlmConfig;
import llm.ModelInstaller;
import llm.ModelType;
import test.framework.Assert;
import test.framework.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

// Tests unitaires pour l'onboarding pour débutants et gestionnaire d'installation
public class ModelInstallerTest {

    @Test
    public void testIsModelInstalledWithTemporaryDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("test_models_dir");
        try {
            ModelType qwen = ModelType.QWEN_CODER;
            Assert.assertFalse(ModelInstaller.isModelInstalled(qwen, tempDir.toString()), "Non installé initialement");

            // Crée un faux fichier de modèle
            Path modelFile = tempDir.resolve(qwen.getNomFichier());
            Files.writeString(modelFile, "DUMMY_MODEL_CONTENT");

            Assert.assertTrue(ModelInstaller.isModelInstalled(qwen, tempDir.toString()), "Doit être détecté comme installé");
        } finally {
            // Nettoyage
            for (Path p : Files.newDirectoryStream(tempDir)) {
                Files.deleteIfExists(p);
            }
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    public void testAfficherMessageOnboardingDebutantsContenu() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        ModelInstaller.afficherMessageOnboardingDebutants(ps, ModelType.QWEN_CODER);
        String output = baos.toString();

        Assert.assertContains(output, "*------------------------------------------*", "Bordure supérieure de style Menu");
        Assert.assertContains(output, "*  Prompting tool 4 a better work from AI  *", "Titre exact de Menu.java");
        Assert.assertContains(output, "*      INSTALLATION DES MODELES LOCAUX     *", "Titre de section");
        Assert.assertContains(output, "100% Hors-ligne & Prive", "Mention confidentialité");
        Assert.assertContains(output, "100% Gratuit", "Mention gratuité");
        Assert.assertContains(output, "Espace disque requis", "Mention espace disque");
        Assert.assertContains(output, "Qwen 2.5 Coder", "Mention modèle recommandé");
        Assert.assertContains(output, "Voulez-vous autoriser le telechargement", "Demande d'autorisation");
    }

    @Test
    public void testDemanderPermissionUtilisateurAccepteOption1() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes()));
        LlmConfig config = new LlmConfig();

        ModelType result = ModelInstaller.demanderPermissionUtilisateur(scanner, ps, ModelType.GEMMA_GENERAL, config);

        Assert.assertEquals(ModelType.GEMMA_GENERAL, result, "Option 1 sélectionne le modèle recommandé");
        Assert.assertTrue(config.isPermissionAccordee(), "La permission doit être accordée");
    }

    @Test
    public void testDemanderPermissionUtilisateurChoixOption2() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Choix 2 (Choisir), puis sélectionne le modèle numéro 1 (Qwen Coder)
        Scanner scanner = new Scanner(new ByteArrayInputStream("2\n1\n".getBytes()));
        LlmConfig config = new LlmConfig();

        ModelType result = ModelInstaller.demanderPermissionUtilisateur(scanner, ps, ModelType.GEMMA_GENERAL, config);

        Assert.assertEquals(ModelType.QWEN_CODER, result, "Option 2 + 1 sélectionne Qwen Coder");
        Assert.assertTrue(config.isPermissionAccordee(), "La permission doit être accordée");
        Assert.assertEquals("qwen-coder", config.getModeleParDefaut(), "Modele par défaut mis à jour");
    }

    @Test
    public void testDemanderPermissionUtilisateurRefuseOption3() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("3\n".getBytes()));
        LlmConfig config = new LlmConfig();

        ModelType result = ModelInstaller.demanderPermissionUtilisateur(scanner, ps, ModelType.QWEN_CODER, config);

        Assert.assertNull(result, "Option 3 refuse et renvoie null");
        Assert.assertFalse(config.isPermissionAccordee(), "La permission ne doit pas être accordée");
    }

    // --- Verification d'integrite des modeles telecharges (Issue #59) ---

    public void testCalculerSha256SurVecteurConnu() throws Exception {
        Path fichier = Files.createTempFile("sha_test", ".bin");
        try {
            Files.writeString(fichier, "abc");
            // Vecteur de test de reference NIST pour SHA-256("abc")
            Assert.assertEquals(
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    ModelInstaller.calculerSha256(fichier),
                    "SHA-256 du vecteur de test 'abc'");
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

    public void testCalculerSha256SurFichierVide() throws Exception {
        Path fichier = Files.createTempFile("sha_vide", ".bin");
        try {
            Assert.assertEquals(
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    ModelInstaller.calculerSha256(fichier),
                    "SHA-256 du fichier vide");
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

    public void testChaqueModeleDuRegistrePossedeUneEmpreinteDeReference() {
        for (ModelType model : ModelType.getAllAvailable()) {
            Assert.assertNotNull(model.getSha256(), "Empreinte declaree pour " + model.getId());
            Assert.assertMatchesRegex(model.getSha256(), "^[0-9a-f]{64}$",
                    "Empreinte SHA-256 bien formee pour " + model.getId());
            Assert.assertStrictPositive(model.getTailleOctets(), "Taille de reference pour " + model.getId());
        }
    }

    // L'URL doit etre epinglee sur une revision precise : une reference mobile ("main")
    // invaliderait l'empreinte des que le depot amont republie le fichier.
    public void testUrlDeTelechargementEpingleeSurUneRevision() {
        for (ModelType model : ModelType.getAllAvailable()) {
            Assert.assertNotContains(model.getUrlTelechargement(), "/resolve/main/",
                    "URL epinglee sur une revision pour " + model.getId());
            Assert.assertMatchesRegex(model.getUrlTelechargement(), ".*/resolve/[0-9a-f]{40}/.*",
                    "Revision de 40 caracteres dans l'URL de " + model.getId());
        }
    }

    public void testVerifierIntegriteRejetteUnFichierDeTailleIncorrecte() throws Exception {
        Path fichier = Files.createTempFile("modele_tronque", ".gguf");
        try {
            Files.writeString(fichier, "contenu tronque");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            boolean ok = ModelInstaller.verifierIntegrite(ModelType.QWEN_CODER, fichier, new PrintStream(baos));

            Assert.assertFalse(ok, "Un fichier de taille incorrecte doit etre rejete");
            Assert.assertContains(baos.toString(), "Taille inattendue", "Cause du rejet explicitee");
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

    public void testVerifierIntegriteRejetteUneEmpreinteInvalide() throws Exception {
        Path fichier = Files.createTempFile("modele_corrompu", ".gguf");
        try {
            // Taille exacte attendue mais contenu different : seule l'empreinte peut le detecter
            Files.writeString(fichier, "abc");
            String empreinteDunAutreContenu = "0000000000000000000000000000000000000000000000000000000000000000";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            boolean ok = ModelInstaller.verifierIntegrite(fichier, empreinteDunAutreContenu, 3L, new PrintStream(baos));

            Assert.assertFalse(ok, "Un contenu ne correspondant pas a l'empreinte doit etre rejete");
            Assert.assertContains(baos.toString(), "EMPREINTE INVALIDE", "Cause du rejet explicitee");
            Assert.assertNotContains(baos.toString(), "Taille inattendue", "Le rejet vient bien de l'empreinte, pas de la taille");
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

    public void testVerifierIntegriteAccepteUnFichierConforme() throws Exception {
        Path fichier = Files.createTempFile("modele_valide", ".gguf");
        try {
            Files.writeString(fichier, "abc");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            boolean ok = ModelInstaller.verifierIntegrite(fichier,
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", 3L,
                    new PrintStream(baos));

            Assert.assertTrue(ok, "Un fichier conforme doit etre accepte");
            Assert.assertContains(baos.toString(), "Integrite confirmee", "Confirmation affichee");
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

    // L'empreinte reste verifiee meme si la taille de reference est inconnue (0)
    public void testVerifierIntegriteSansTailleDeReference() throws Exception {
        Path fichier = Files.createTempFile("modele_sans_taille", ".gguf");
        try {
            Files.writeString(fichier, "abc");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            boolean ok = ModelInstaller.verifierIntegrite(fichier,
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", 0L,
                    new PrintStream(baos));

            Assert.assertTrue(ok, "La taille inconnue ne doit pas bloquer la verification d'empreinte");
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

}
