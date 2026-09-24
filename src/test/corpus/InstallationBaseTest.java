package test.corpus;

import corpus.InstallationBase;
import llm.ModelInstaller;
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

// Installation de la base : aucun test n'atteint le reseau, l'archive est fabriquee sur place
public class InstallationBaseTest {

    private Path racine;
    private Path archive;
    private PrintStream sortie;
    private int telechargements;

    @BeforeEach
    public void preparer() throws Exception {
        racine = Files.createTempDirectory("installation_base");
        archive = Files.createTempFile("base", ".zip");
        sortie = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        telechargements = 0;
        try (OutputStream flux = Files.newOutputStream(archive); ZipOutputStream zip = new ZipOutputStream(flux)) {
            zip.putNextEntry(new ZipEntry("themes/sommaire.json"));
            zip.write("{\"version\":2,\"langues\":{}}".getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("themes/fr/cuisine.json"));
            zip.write("[\n]\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    @AfterEach
    public void nettoyer() throws Exception {
        Files.deleteIfExists(archive);
        try (var flux = Files.walk(racine)) {
            flux.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }
    }

    private InstallationBase.Telechargeur copie(boolean verifier) {
        return (url, cible, out, listener, verification) -> {
            telechargements++;
            try {
                Files.copy(archive, cible);
            } catch (Exception e) {
                return false;
            }
            return !verifier || verification.test(cible);
        };
    }

    @Test
    public void testArchiveExtraite() throws Exception {
        Path base = racine.resolve("themes");
        String empreinte = ModelInstaller.calculerSha256(archive);
        boolean ok = InstallationBase.installer(base, "url", empreinte, Files.size(archive), copie(true), sortie, null);

        Assert.assertTrue(ok, "Installee");
        Assert.assertTrue(Files.exists(base.resolve("sommaire.json")), "Sommaire en place");
        Assert.assertTrue(Files.exists(base.resolve("fr/cuisine.json")), "Fichiers de theme en place");
        Assert.assertFalse(Files.exists(racine.resolve(InstallationBase.ARCHIVE)), "Archive supprimee");
    }

    @Test
    public void testEmpreinteInvalideRienInstalle() {
        Path base = racine.resolve("themes");
        boolean ok = InstallationBase.installer(base, "url", "0".repeat(64), 0, copie(true), sortie, null);

        Assert.assertFalse(ok, "Refusee");
        Assert.assertFalse(Files.exists(base), "Rien extrait");
    }

    @Test
    public void testDejaInstalleeSansTelechargement() throws Exception {
        Path base = racine.resolve("themes");
        Files.createDirectories(base);
        Files.writeString(base.resolve("sommaire.json"), "{}");

        Assert.assertTrue(InstallationBase.installer(base, "url", "x", 0, copie(false), sortie, null), "Deja la");
        Assert.assertEquals(0, telechargements, "Aucun telechargement");
    }
}
