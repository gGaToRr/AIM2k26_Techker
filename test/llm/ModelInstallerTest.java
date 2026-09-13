package test.llm;

import llm.LlmConfig;
import llm.ModelInstaller;
import llm.ModelType;
import test.framework.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

// Tests unitaires pour l'onboarding pour débutants et gestionnaire d'installation
public class ModelInstallerTest {

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

    public void testDemanderPermissionUtilisateurAccepteOption1() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes()));
        LlmConfig config = new LlmConfig();

        ModelType result = ModelInstaller.demanderPermissionUtilisateur(scanner, ps, ModelType.GEMMA_GENERAL, config);

        Assert.assertEquals(ModelType.GEMMA_GENERAL, result, "Option 1 sélectionne le modèle recommandé");
        Assert.assertTrue(config.isPermissionAccordee(), "La permission doit être accordée");
    }

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

    public void testDemanderPermissionUtilisateurRefuseOption3() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Scanner scanner = new Scanner(new ByteArrayInputStream("3\n".getBytes()));
        LlmConfig config = new LlmConfig();

        ModelType result = ModelInstaller.demanderPermissionUtilisateur(scanner, ps, ModelType.QWEN_CODER, config);

        Assert.assertNull(result, "Option 3 refuse et renvoie null");
        Assert.assertFalse(config.isPermissionAccordee(), "La permission ne doit pas être accordée");
    }
}
