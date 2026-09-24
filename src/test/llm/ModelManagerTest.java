package test.llm;

import cli.CliArgs;
import llm.LlmConfig;
import llm.ModelManager;
import llm.ModelType;
import llm.ModelsCommand;
import test.framework.AfterEach;
import test.framework.Assert;
import test.framework.BeforeEach;
import test.framework.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;

// Tests du cycle de vie des modeles locaux (Issue #45).
//
// Tout se passe dans un repertoire temporaire : aucun test ne doit pouvoir
// supprimer les modeles reels de la machine.
public class ModelManagerTest {

    private Path repertoire;
    private ByteArrayOutputStream tampon;
    private PrintStream sortie;
    // La purge sauvegarde la config sous user.dir : on le redirige vers le
    // repertoire temporaire pour ne pas ecraser le .llm_config/ reel du projet.
    private String userDirOriginal;

    @BeforeEach
    public void preparer() throws Exception {
        repertoire = Files.createTempDirectory("modeles_crud");
        userDirOriginal = System.getProperty("user.dir");
        System.setProperty("user.dir", repertoire.toString());
        tampon = new ByteArrayOutputStream();
        sortie = new PrintStream(tampon);
    }

    @AfterEach
    public void nettoyer() throws Exception {
        System.setProperty("user.dir", userDirOriginal);
        if (repertoire != null && Files.exists(repertoire)) {
            try (var flux = Files.walk(repertoire)) {
                flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    private Path installerFaux(ModelType model, int octets) throws Exception {
        Path fichier = repertoire.resolve(model.getNomFichier());
        Files.write(fichier, new byte[octets]);
        return fichier;
    }

    // Moteur llama.cpp simule : jamais de telechargement reel pendant les tests
    private static final class MoteurEspion implements ModelsCommand.Moteur {
        boolean present;
        boolean reussit = true;
        int installations = 0;
        MoteurEspion(boolean present) { this.present = present; }
        @Override public boolean present() { return present; }
        @Override public boolean installer(PrintStream out, llm.ModelInstaller.DownloadProgressListener listener) {
            installations++;
            if (listener != null) listener.onProgress(10, 20, 1.0);
            if (!reussit) out.println("[!] Plateforme non prise en charge");
            present = reussit;
            return reussit;
        }
    }

    private static Scanner reponse(String saisie) {
        return new Scanner(new ByteArrayInputStream(saisie.getBytes()));
    }

    private LlmConfig config() {
        LlmConfig config = new LlmConfig();
        config.setRepertoireModeles(repertoire.toString());
        return config;
    }

    // --- Inventaire ---

    @Test
    public void testInventaireCouvreTousLesModelesDuRegistre() {
        Assert.assertSize(ModelManager.inventaire(repertoire.toString()),
                ModelType.getAllAvailable().size(), "Un etat par modele declare");
    }

    @Test
    public void testStatutRefleteLaPresenceDuFichier() throws Exception {
        Assert.assertFalse(ModelManager.statutDe(ModelType.QWEN_CODER, repertoire.toString()).installe(),
                "Absent au depart");

        installerFaux(ModelType.QWEN_CODER, 2048);
        ModelManager.ModelStatus etat = ModelManager.statutDe(ModelType.QWEN_CODER, repertoire.toString());

        Assert.assertTrue(etat.installe(), "Present apres depot du fichier");
        Assert.assertEquals(2048L, etat.tailleOctets(), "Taille reelle lue sur le disque");
        Assert.assertTrue(etat.dateInstallation().isPresent(), "Date d'installation renseignee");
    }

    @Test
    public void testEspaceTotalNAdditionneQueLesModelesInstalles() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        installerFaux(ModelType.SMOLLM_FAST, 500);

        Assert.assertEquals(1500L, ModelManager.espaceTotalOccupe(repertoire.toString()),
                "Somme des seuls modeles presents");
    }

    // --- Suppression unitaire ---

    @Test
    public void testSuppressionRetireLeFichierEtLuiSeul() throws Exception {
        Path qwen = installerFaux(ModelType.QWEN_CODER, 1000);
        Path smollm = installerFaux(ModelType.SMOLLM_FAST, 500);

        Assert.assertTrue(ModelManager.supprimer(ModelType.QWEN_CODER, repertoire.toString(), sortie),
                "Suppression aboutie");
        Assert.assertFalse(Files.exists(qwen), "Le modele vise est supprime");
        Assert.assertTrue(Files.exists(smollm), "Aucun effet de bord sur les autres modeles");
    }

    // Un telechargement interrompu laisse un .part : l'espace ne serait pas reellement libere
    @Test
    public void testSuppressionNettoieAussiLeFichierPartiel() throws Exception {
        Path complet = installerFaux(ModelType.QWEN_CODER, 1000);
        Path partiel = Path.of(complet + ".part");
        Files.write(partiel, new byte[300]);

        ModelManager.supprimer(ModelType.QWEN_CODER, repertoire.toString(), sortie);

        Assert.assertFalse(Files.exists(partiel), "Le fichier .part residuel est nettoye");
    }

    @Test
    public void testSuppressionDUnModeleAbsentNEstPasUneErreur() {
        Assert.assertFalse(ModelManager.supprimer(ModelType.GEMMA_GENERAL, repertoire.toString(), sortie),
                "Rien a supprimer");
        Assert.assertContains(tampon.toString(), "n'est pas installe", "Message explicite");
    }

    // --- Purge ---

    @Test
    public void testPurgeSupprimeTousLesModelesInstalles() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        installerFaux(ModelType.SMOLLM_FAST, 500);
        installerFaux(ModelType.GEMMA_GENERAL, 700);

        int supprimes = ModelManager.purger(repertoire.toString(), config(), sortie);

        Assert.assertEquals(3, supprimes, "Trois modeles supprimes");
        Assert.assertEquals(0L, ModelManager.espaceTotalOccupe(repertoire.toString()), "Plus rien sur le disque");
    }

    // Reconduire tacitement la permission relancerait un telechargement de plusieurs Go
    @Test
    public void testPurgeReinitialiseLAutorisationDeTelechargement() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        LlmConfig config = config();
        config.setPermissionAccordee(true);

        ModelManager.purger(repertoire.toString(), config, sortie);

        Assert.assertFalse(config.isPermissionAccordee(),
                "L'autorisation doit etre redemandee apres une purge");
    }

    @Test
    public void testPurgeSansModeleNeCassePas() {
        Assert.assertEquals(0, ModelManager.purger(repertoire.toString(), config(), sortie),
                "Aucun modele a supprimer");
    }

    // --- Confirmation : aucune suppression sans accord explicite ---

    @Test
    public void testSuppressionRefuseeLaisseLeFichierIntact() throws Exception {
        Path qwen = installerFaux(ModelType.QWEN_CODER, 1000);
        CliArgs options = CliArgs.builder().modelsDelete("qwen").build();

        int code = ModelsCommand.executer(options, config(), sortie, reponse("n\n"));

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Un refus n'est pas une erreur");
        Assert.assertTrue(Files.exists(qwen), "Le fichier survit au refus");
        Assert.assertContains(tampon.toString(), "annulee", "Annulation annoncee");
    }

    // Une reponse vide vaut refus : la confirmation est en [o/N]
    @Test
    public void testReponseVideVautRefus() throws Exception {
        Path qwen = installerFaux(ModelType.QWEN_CODER, 1000);
        CliArgs options = CliArgs.builder().modelsDelete("qwen").build();

        ModelsCommand.executer(options, config(), sortie, reponse("\n"));

        Assert.assertTrue(Files.exists(qwen), "Sans accord explicite, rien n'est supprime");
    }

    @Test
    public void testPurgeRefuseeLaisseToutIntact() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        installerFaux(ModelType.SMOLLM_FAST, 500);
        CliArgs options = CliArgs.builder().modelsPurge(true).build();

        ModelsCommand.executer(options, config(), sortie, reponse("n\n"));

        Assert.assertEquals(1500L, ModelManager.espaceTotalOccupe(repertoire.toString()),
                "Rien n'est supprime sans accord");
    }

    @Test
    public void testSuppressionAccepteeRetireLeModele() throws Exception {
        Path qwen = installerFaux(ModelType.QWEN_CODER, 1000);
        CliArgs options = CliArgs.builder().modelsDelete("qwen").build();

        int code = ModelsCommand.executer(options, config(), sortie, reponse("o\n"));

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Suppression aboutie");
        Assert.assertFalse(Files.exists(qwen), "Le modele est bien supprime");
    }

    // --- Commandes CLI ---

    @Test
    public void testCommandeListeAfficheStatutTailleEtRepertoire() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 2_000_000);

        ModelsCommand.executer(CliArgs.builder().modelsList(true).build(), config(), sortie, reponse(""));
        String rendu = tampon.toString();

        Assert.assertContains(rendu, "qwen-coder", "Identifiant liste");
        Assert.assertContains(rendu, "installe", "Statut installe");
        Assert.assertContains(rendu, "absent", "Statut absent pour les autres");
        Assert.assertContains(rendu, "Espace occupe", "Total d'espace affiche");
    }

    @Test
    public void testCommandeInfoAfficheLaFicheTechnique() {
        ModelsCommand.executer(CliArgs.builder().modelsInfo("deepseek").build(), config(), sortie, reponse(""));
        String rendu = tampon.toString();

        Assert.assertContains(rendu, ModelType.DEEPSEEK_REASONING.getNomAffiche(), "Nom du modele");
        Assert.assertContains(rendu, ModelType.DEEPSEEK_REASONING.getRamRecommandee(), "RAM conseillee");
        Assert.assertContains(rendu, "Chemin local", "Chemin sur disque");
        Assert.assertContains(rendu, repertoire.toString(), "Chemin pointant vers le repertoire configure");
    }

    @Test
    public void testModeleInconnuRendUnCodeDErreurEtLaListe() {
        int code = ModelsCommand.executer(CliArgs.builder().modelsInfo("modele_imaginaire").build(),
                config(), sortie, reponse(""));

        Assert.assertEquals(ModelsCommand.ECHEC, code, "Code de sortie non nul");
        Assert.assertContains(tampon.toString(), "qwen-coder", "Les identifiants valides sont rappeles");
    }

    // Les alias du registre doivent fonctionner comme pour -m/--model
    @Test
    public void testLesAliasSontAcceptes() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);

        for (String alias : new String[]{"qwen", "qwen-coder", "code", "QWEN"}) {
            ByteArrayOutputStream local = new ByteArrayOutputStream();
            int code = ModelsCommand.executer(CliArgs.builder().modelsInfo(alias).build(),
                    config(), new PrintStream(local), reponse(""));
            Assert.assertEquals(ModelsCommand.SUCCES, code, "Alias accepte : " + alias);
        }
    }

    // --- Installation (-mt / --models-install) ---

    @Test
    public void testParserReconnaitModelsInstall() {
        CliArgs args = cli.CliParser.parse(new String[]{"-mt", "qwen"});
        Assert.assertEquals("qwen", args.modelsInstall(), "Valeur de -mt");
        Assert.assertTrue(args.isCommandeModeles(), "Commande de gestion des modeles");

        CliArgs longue = cli.CliParser.parse(new String[]{"--models-install", "deepseek"});
        Assert.assertEquals("deepseek", longue.modelsInstall(), "Valeur de --models-install");
    }

    @Test
    public void testInstallerUnModeleInconnuEchoue() {
        int code = ModelsCommand.executer(CliArgs.builder().modelsInstall("modele_imaginaire").build(),
                config(), sortie, reponse(""));

        Assert.assertEquals(ModelsCommand.ECHEC, code, "Code de sortie non nul");
        Assert.assertContains(tampon.toString(), "qwen-coder", "Les identifiants valides sont rappeles");
    }

    // Un modele deja present ne doit declencher aucun telechargement
    @Test
    public void testInstallerUnModeleDejaPresentNeTelechargeRien() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        LlmConfig config = config();
        config.setPermissionAccordee(true); // evite la sauvegarde dans le .llm_config/ reel

        MoteurEspion moteur = new MoteurEspion(true);
        int code = ModelsCommand.executer(CliArgs.builder().modelsInstall("qwen").build(),
                config, sortie, reponse(""), moteur);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes");
        Assert.assertContains(tampon.toString(), "deja installe", "Signale le modele deja present");
        Assert.assertEquals(0, moteur.installations, "Moteur present : rien a installer");
    }

    // Clone neuf : modeles telecharges mais dossier llama/ absent. Installer un modele
    // installe aussi le moteur, sinon aucun modele ne peut tourner.
    @Test
    public void testInstallerUnModeleInstalleLeMoteurAbsent() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        LlmConfig config = config();
        config.setPermissionAccordee(true);
        MoteurEspion moteur = new MoteurEspion(false);

        int code = ModelsCommand.executer(CliArgs.builder().modelsInstall("qwen").build(),
                config, sortie, reponse(""), moteur);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes");
        Assert.assertEquals(1, moteur.installations, "Moteur installe avec le modele");
    }

    // Echec du moteur : signale a l'extension, qui ne doit pas croire le modele utilisable
    @Test
    public void testEchecDuMoteurSignaleEnJson() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        LlmConfig config = config();
        config.setPermissionAccordee(true);
        MoteurEspion moteur = new MoteurEspion(false);
        moteur.reussit = false;

        int code = ModelsCommand.executer(CliArgs.builder().modelsInstall("qwen").output("json").build(),
                config, sortie, reponse(""), moteur);

        Assert.assertEquals(ModelsCommand.ECHEC, code, "Echec");
        Assert.assertContains(tampon.toString(), "\"type\":\"fin\",\"ok\":false", "Fin en echec");
        Assert.assertContains(tampon.toString(), "Plateforme non prise en charge", "Cause transmise");
    }

    // Clone neuf : la base de prompts s'installe avec le premier modele, comme le moteur
    @Test
    public void testInstallerUnModeleInstalleLaBaseAbsente() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        LlmConfig config = config();
        config.setPermissionAccordee(true);
        MoteurEspion base = new MoteurEspion(false);

        int code = ModelsCommand.executer(CliArgs.builder().modelsInstall("qwen").build(),
                config, sortie, reponse(""), new MoteurEspion(true), base);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes");
        Assert.assertEquals(1, base.installations, "Base installee avec le modele");
    }

    // Base indisponible (archive non publiee, pas de reseau) : le modele reste installe
    @Test
    public void testEchecDeLaBaseNeBloquePasLeModele() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        LlmConfig config = config();
        config.setPermissionAccordee(true);
        MoteurEspion base = new MoteurEspion(false);
        base.reussit = false;

        int code = ModelsCommand.executer(CliArgs.builder().modelsInstall("qwen").output("json").build(),
                config, sortie, reponse(""), new MoteurEspion(true), base);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Modele installe malgre la base");
        Assert.assertContains(tampon.toString(), "\"type\":\"fin\",\"ok\":true", "Fin en succes pour l'extension");
    }

    @Test
    public void testParserReconnaitCorpusInstall() {
        Assert.assertTrue(cli.CliParser.parse(new String[]{"-ci"}).isCorpusInstall(), "Drapeau -ci");
        Assert.assertTrue(cli.CliParser.parse(new String[]{"--corpus-install"}).isCommandeModeles(), "Commande d'installation");
    }

    @Test
    public void testParserReconnaitRuntimeInstall() {
        CliArgs args = cli.CliParser.parse(new String[]{"-ri"});
        Assert.assertTrue(args.isRuntimeInstall(), "Drapeau -ri");
        Assert.assertTrue(args.isCommandeModeles(), "Commande de gestion des modeles");
        Assert.assertTrue(cli.CliParser.parse(new String[]{"--runtime-install"}).isRuntimeInstall(),
                "Drapeau --runtime-install");
    }

    // --runtime-install -o json : progression puis fin, comme un modele (page Modeles)
    @Test
    public void testInstallerLeMoteurEnJson() {
        MoteurEspion moteur = new MoteurEspion(false);

        int code = ModelsCommand.executer(CliArgs.builder().runtimeInstall(true).output("json").build(),
                config(), sortie, reponse(""), moteur);

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Succes");
        Assert.assertEquals(1, moteur.installations, "Une installation");
        Assert.assertContains(tampon.toString(), "\"type\":\"progression\",\"lus\":10", "Progression relayee");
        Assert.assertContains(tampon.toString(), "\"type\":\"fin\",\"ok\":true", "Fin en succes");
    }

    // --- Verification (-mc / --models-check) ---

    // Backend espion : compte les appels, pour prouver qu'aucune generation n'est lancee a tort
    private static final class BackendEspion implements llm.LlmBackend {
        int appels = 0;
        @Override public boolean isAvailable(ModelType model, String modelsDir) { return true; }
        @Override public GenerationResult generate(ModelType model, String prompt, LlmConfig config, TokenConsumer consumer) {
            appels++;
            return new GenerationResult("ok", 1, 1, 1.0, model);
        }
    }

    @Test
    public void testVerificationSansModeleInstalle() {
        BackendEspion backend = new BackendEspion();
        int fonctionnels = ModelManager.verifierModeles(repertoire.toString(), config(), backend, true, sortie);

        Assert.assertEquals(0, fonctionnels, "Aucun modele fonctionnel");
        Assert.assertContains(tampon.toString(), "Aucun modele installe", "Message explicite");
        Assert.assertEquals(0, backend.appels, "Aucune generation lancee");
    }

    // Un fichier tronque ne doit jamais etre soumis au moteur : il planterait llama.cpp
    @Test
    public void testFichierDeTailleIncorrecteEstSignaleSansGeneration() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        BackendEspion backend = new BackendEspion();

        int fonctionnels = ModelManager.verifierModeles(repertoire.toString(), config(), backend, true, sortie);

        Assert.assertEquals(0, fonctionnels, "Modele endommage non fonctionnel");
        Assert.assertContains(tampon.toString(), "endommage", "Fichier signale endommage");
        Assert.assertContains(tampon.toString(), "Modeles installes : 1", "Le modele est bien compte comme installe");
        Assert.assertEquals(0, backend.appels, "Pas de generation sur un fichier endommage");
    }

    // --- Sorties pour l'extension (-o json, -y) ---

    @Test
    public void testInventaireJsonDecritChaqueModele() throws Exception {
        installerFaux(ModelType.QWEN_CODER, 1000);
        String json = ModelManager.inventaireEnJson(repertoire.toString(), false);

        Assert.assertContains(json, "\"id\":\"qwen-coder\"", "Identifiant present");
        Assert.assertContains(json, "\"installe\":true,\"tailleOctets\":1000", "Modele installe avec sa taille reelle");
        Assert.assertContains(json, "\"id\":\"gemma-general\"", "Les modeles absents sont listes aussi");
        Assert.assertContains(json, "\"installe\":false", "Absent signale");
        Assert.assertContains(json, "\"moteur\":{\"installe\":false", "Etat du moteur llama.cpp");
    }

    // L'extension confirme dans sa propre fenetre : -y ne doit plus rien demander
    @Test
    public void testSuppressionAvecOuiNeDemandePasDeConfirmation() throws Exception {
        Path qwen = installerFaux(ModelType.QWEN_CODER, 1000);
        CliArgs options = CliArgs.builder().modelsDelete("qwen").yes(true).output("json").build();

        int code = ModelsCommand.executer(options, config(), sortie, reponse(""));

        Assert.assertEquals(ModelsCommand.SUCCES, code, "Suppression reussie");
        Assert.assertFalse(Files.exists(qwen), "Fichier supprime sans saisie");
        Assert.assertContains(tampon.toString(), "\"supprime\":true", "Reponse JSON");
    }

    @Test
    public void testParserReconnaitModelsCheck() {
        CliArgs args = cli.CliParser.parse(new String[]{"--models-check"});
        Assert.assertTrue(args.isModelsCheck(), "Drapeau long");
        Assert.assertTrue(args.isCommandeModeles(), "Commande de gestion des modeles");
        Assert.assertTrue(cli.CliParser.parse(new String[]{"-mc"}).isModelsCheck(), "Drapeau court");
    }

    @Test
    public void testFormatageDesTailles() {
        Assert.assertEquals("0 o", ModelManager.formaterOctets(0), "Zero");
        Assert.assertEquals("512 o", ModelManager.formaterOctets(512), "Octets");
        Assert.assertEquals("1.0 Ko", ModelManager.formaterOctets(1024), "Kilo-octets");
        Assert.assertEquals("1.0 Mo", ModelManager.formaterOctets(1024 * 1024), "Mega-octets");
        Assert.assertEquals("1.00 Go", ModelManager.formaterOctets(1024L * 1024 * 1024), "Giga-octets");
    }
}
