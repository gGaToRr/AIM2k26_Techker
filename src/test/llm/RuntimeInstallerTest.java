package test.llm;

import llm.RuntimeInstaller;
import test.framework.AfterEach;
import test.framework.Assert;
import test.framework.BeforeEach;
import test.framework.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Installation du moteur llama.cpp : aucun test n'atteint le reseau, le telechargement
// est remplace par la copie d'une archive fabriquee sur place.
public class RuntimeInstallerTest {

    private Path racine;
    private Path archives;
    private ByteArrayOutputStream tampon;
    private PrintStream sortie;
    private int telechargements;

    @BeforeEach
    public void preparer() throws Exception {
        racine = Files.createTempDirectory("moteur_llama");
        archives = Files.createTempDirectory("moteur_archives");
        tampon = new ByteArrayOutputStream();
        sortie = new PrintStream(tampon, true, StandardCharsets.UTF_8);
        telechargements = 0;
    }

    @AfterEach
    public void nettoyer() throws Exception {
        for (Path dossier : new Path[] {racine, archives}) {
            try (var flux = Files.walk(dossier)) {
                flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    // "Telecharge" une archive locale : copie, puis verification demandee par l'installateur
    private RuntimeInstaller.Telechargeur copie(Path source) {
        return (url, cible, out, listener, verification) -> {
            telechargements++;
            try {
                Files.copy(source, cible);
            } catch (Exception e) {
                return false;
            }
            return verification.test(cible) || supprimer(cible);
        };
    }

    // Verification ignoree : les archives de test n'ont pas l'empreinte de la vraie release
    private RuntimeInstaller.Telechargeur copieSansVerification(Path source) {
        return (url, cible, out, listener, verification) -> {
            telechargements++;
            try {
                Files.copy(source, cible);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static boolean supprimer(Path fichier) {
        fichier.toFile().delete();
        return false;
    }

    private Path zip(String... entrees) throws Exception {
        Path archive = archives.resolve("moteur.zip");
        try (OutputStream flux = Files.newOutputStream(archive); ZipOutputStream zip = new ZipOutputStream(flux)) {
            for (String entree : entrees) {
                zip.putNextEntry(new ZipEntry(entree));
                zip.write("binaire".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive;
    }

    @Test
    public void testPlateformes() {
        Assert.assertEquals("linux-x64", RuntimeInstaller.plateforme("Linux", "amd64"), "Linux x64");
        Assert.assertEquals("macos-arm64", RuntimeInstaller.plateforme("Mac OS X", "aarch64"), "macOS Apple Silicon");
        Assert.assertEquals("windows-x64", RuntimeInstaller.plateforme("Windows 11", "amd64"), "Windows x64");
        Assert.assertEquals("inconnu", RuntimeInstaller.plateforme("SunOS", "sparc"), "Plateforme inconnue");
        Assert.assertTrue(RuntimeInstaller.archivePour("Linux", "amd64").get().nom().endsWith("ubuntu-x64.tar.gz"),
                "Build CPU Ubuntu");
        Assert.assertTrue(RuntimeInstaller.archivePour("Windows 11", "amd64").get().estZip(), "Zip sous Windows");
    }

    @Test
    public void testPlateformeInconnueSansTelechargement() throws Exception {
        boolean ok = RuntimeInstaller.installer(racine, "SunOS", "sparc", copie(zip("x")), sortie, null);

        Assert.assertFalse(ok, "Echec");
        Assert.assertEquals(0, telechargements, "Rien telecharge");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "non prise en charge", "Cause expliquee");
    }

    @Test
    public void testMoteurDejaInstalleSansTelechargement() throws Exception {
        Path destination = RuntimeInstaller.repertoireInstallation(racine);
        Files.createDirectories(destination);
        Path executable = Files.createFile(RuntimeInstaller.executable(destination, false));
        executable.toFile().setExecutable(true);

        boolean ok = RuntimeInstaller.installer(racine, "Linux", "amd64", copie(zip("x")), sortie, null);

        Assert.assertTrue(ok, "Succes");
        Assert.assertEquals(0, telechargements, "Rien telecharge");
    }

    // Archive qui ne correspond pas a l'empreinte : rien n'est extrait ni garde
    @Test
    public void testEmpreinteInvalideRienInstalle() throws Exception {
        boolean ok = RuntimeInstaller.installer(racine, "Windows 11", "amd64",
                copie(zip("llama-completion.exe")), sortie, null);

        Assert.assertFalse(ok, "Echec");
        Assert.assertFalse(Files.exists(RuntimeInstaller.repertoireInstallation(racine)), "Rien extrait");
        try (var fichiers = Files.list(racine)) {
            Assert.assertEquals(0L, fichiers.count(), "Archive supprimee");
        }
    }

    @Test
    public void testZipWindowsExtrait() throws Exception {
        boolean ok = RuntimeInstaller.installer(racine, "Windows 11", "amd64",
                copieSansVerification(zip("llama-completion.exe", "ggml-base.dll")), sortie, null);

        Path destination = RuntimeInstaller.repertoireInstallation(racine);
        Assert.assertTrue(ok, "Succes");
        Assert.assertTrue(Files.exists(destination.resolve("llama-completion.exe")), "Executable extrait");
        Assert.assertTrue(Files.exists(destination.resolve("ggml-base.dll")), "Bibliotheques extraites");
    }

    // Une entree "../" ne doit jamais ecrire hors du dossier d'installation
    @Test
    public void testZipHorsDossierRefuse() throws Exception {
        boolean ok = RuntimeInstaller.installer(racine, "Windows 11", "amd64",
                copieSansVerification(zip("../evade.exe")), sortie, null);

        Assert.assertFalse(ok, "Echec");
        Assert.assertFalse(Files.exists(racine.resolve("evade.exe")), "Rien ecrit hors du dossier");
        Assert.assertContains(tampon.toString(StandardCharsets.UTF_8), "hors du dossier", "Cause expliquee");
    }

    // Archive Linux / macOS : dossier llama-<version>/ retire, droit d'execution garde
    @Test
    public void testTarGzExtraitSansDossierRacine() throws Exception {
        Path contenu = archives.resolve("llama-" + RuntimeInstaller.VERSION);
        Files.createDirectories(contenu);
        Path binaire = Files.writeString(contenu.resolve("llama-completion"), "#!/bin/sh\n");
        binaire.toFile().setExecutable(true);
        Path archive = archives.resolve("moteur.tar.gz");
        Process tar = new ProcessBuilder("tar", "-czf", archive.toString(), "-C", archives.toString(),
                contenu.getFileName().toString()).start();
        Assert.assertEquals(0, tar.waitFor(), "Archive de test creee");

        boolean ok = RuntimeInstaller.installer(racine, "Linux", "amd64", copieSansVerification(archive), sortie, null);

        Path executable = RuntimeInstaller.executable(RuntimeInstaller.repertoireInstallation(racine), false);
        Assert.assertTrue(ok, "Succes");
        Assert.assertTrue(Files.isExecutable(executable), "llama-completion executable au bon endroit");
    }
}
