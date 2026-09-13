package test.llm;

import llm.RuntimeInstaller;
import test.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Tests unitaires pour l'installation automatique du runtime natif llama-cli (TDD)
public class RuntimeInstallerTest {

    // --- Détection de plateforme ---

    public void testDetecterPlateformeLinuxX64() {
        Assert.assertEquals("linux-x64", RuntimeInstaller.detecterPlateforme("Linux", "amd64"), "Linux amd64");
        Assert.assertEquals("linux-x64", RuntimeInstaller.detecterPlateforme("Linux", "x86_64"), "Linux x86_64");
    }

    public void testDetecterPlateformeLinuxArm64() {
        Assert.assertEquals("linux-arm64", RuntimeInstaller.detecterPlateforme("Linux", "aarch64"), "Linux aarch64");
    }

    public void testDetecterPlateformeMacosArm64() {
        Assert.assertEquals("macos-arm64", RuntimeInstaller.detecterPlateforme("Mac OS X", "aarch64"), "macOS Apple Silicon");
    }

    public void testDetecterPlateformeMacosX64() {
        Assert.assertEquals("macos-x64", RuntimeInstaller.detecterPlateforme("Mac OS X", "x86_64"), "macOS Intel");
    }

    public void testDetecterPlateformeWindowsX64() {
        Assert.assertEquals("windows-x64", RuntimeInstaller.detecterPlateforme("Windows 11", "amd64"), "Windows 11 x64");
    }

    public void testDetecterPlateformeWindowsArm64() {
        Assert.assertEquals("windows-arm64", RuntimeInstaller.detecterPlateforme("Windows 11", "aarch64"), "Windows ARM64");
    }

    public void testDetecterPlateformeInconnue() {
        Assert.assertEquals("inconnu", RuntimeInstaller.detecterPlateforme("SunOS", "sparc"), "OS non supporté");
    }

    // --- Sélection d'asset (fixture = vrais noms d'assets de la release b10948 de ggml-org/llama.cpp) ---

    private static final List<String> ASSETS_REELS = List.of(
            "cudart-llama-bin-win-cuda-12.4-x64.zip",
            "cudart-llama-bin-win-cuda-13.3-x64.zip",
            "cudart-llama-bin-win-cuda-13.4-arm64.zip",
            "llama-b10948-bin-android-arm64.tar.gz",
            "llama-b10948-bin-macos-arm64.tar.gz",
            "llama-b10948-bin-macos-x64.tar.gz",
            "llama-b10948-bin-ubuntu-arm64.tar.gz",
            "llama-b10948-bin-ubuntu-openvino-2026.3.1-x64.tar.gz",
            "llama-b10948-bin-ubuntu-rocm-10.0-x64.tar.gz",
            "llama-b10948-bin-ubuntu-s390x.tar.gz",
            "llama-b10948-bin-ubuntu-sycl-fp16-x64.tar.gz",
            "llama-b10948-bin-ubuntu-sycl-fp32-x64.tar.gz",
            "llama-b10948-bin-ubuntu-vulkan-arm64.tar.gz",
            "llama-b10948-bin-ubuntu-vulkan-x64.tar.gz",
            "llama-b10948-bin-ubuntu-x64.tar.gz",
            "llama-b10948-bin-win-cpu-arm64.zip",
            "llama-b10948-bin-win-cpu-x64.zip",
            "llama-b10948-bin-win-cuda-12.4-x64.zip",
            "llama-b10948-bin-win-cuda-13.3-x64.zip",
            "llama-b10948-bin-win-cuda-13.4-arm64.zip",
            "llama-b10948-bin-win-opencl-adreno-arm64.zip",
            "llama-b10948-bin-win-openvino-2026.3.1-x64.zip",
            "llama-b10948-bin-win-rocm-10.0-x64.zip",
            "llama-b10948-bin-win-sycl-x64.zip",
            "llama-b10948-bin-win-vulkan-x64.zip",
            "llama-b10948-ui.tar.gz",
            "llama-b10948-xcframework.zip"
    );

    public void testChoisirAssetLinuxX64ChoisitBuildCpuSimple() {
        String choisi = RuntimeInstaller.choisirAsset(ASSETS_REELS, "linux-x64");
        Assert.assertEquals("llama-b10948-bin-ubuntu-x64.tar.gz", choisi, "Doit choisir le build CPU simple, pas sycl/vulkan/rocm/openvino");
    }

    public void testChoisirAssetLinuxArm64() {
        String choisi = RuntimeInstaller.choisirAsset(ASSETS_REELS, "linux-arm64");
        Assert.assertEquals("llama-b10948-bin-ubuntu-arm64.tar.gz", choisi, "Doit choisir le build ARM64 simple, pas vulkan");
    }

    public void testChoisirAssetMacosArm64() {
        String choisi = RuntimeInstaller.choisirAsset(ASSETS_REELS, "macos-arm64");
        Assert.assertEquals("llama-b10948-bin-macos-arm64.tar.gz", choisi, "Build Apple Silicon");
    }

    public void testChoisirAssetMacosX64() {
        String choisi = RuntimeInstaller.choisirAsset(ASSETS_REELS, "macos-x64");
        Assert.assertEquals("llama-b10948-bin-macos-x64.tar.gz", choisi, "Build Mac Intel");
    }

    public void testChoisirAssetWindowsX64ExcludesCudaVulkanSycl() {
        String choisi = RuntimeInstaller.choisirAsset(ASSETS_REELS, "windows-x64");
        Assert.assertEquals("llama-b10948-bin-win-cpu-x64.zip", choisi, "Doit exclure cuda/vulkan/sycl/openvino/rocm et prendre le CPU simple");
        Assert.assertNotContains(choisi, "cuda", "Ne doit jamais choisir un build nécessitant un GPU/driver spécifique");
    }

    public void testChoisirAssetWindowsArm64() {
        String choisi = RuntimeInstaller.choisirAsset(ASSETS_REELS, "windows-arm64");
        Assert.assertEquals("llama-b10948-bin-win-cpu-arm64.zip", choisi, "Build Windows ARM64 CPU");
    }

    public void testChoisirAssetRetourneNullSiAucunMatch() {
        String choisi = RuntimeInstaller.choisirAsset(List.of("autre-chose.zip"), "linux-x64");
        Assert.assertNull(choisi, "Aucun asset compatible ne doit renvoyer null plutôt qu'un mauvais choix");
    }

    // --- Parsing JSON minimal de l'API GitHub releases ---

    private static final String JSON_RELEASE_FIXTURE = "[{\n" +
            "  \"tag_name\": \"b10948\",\n" +
            "  \"assets\": [\n" +
            "    {\"id\": 1, \"name\": \"llama-b10948-bin-ubuntu-x64.tar.gz\", \"size\": 12345, \"browser_download_url\": \"https://github.com/ggml-org/llama.cpp/releases/download/b10948/llama-b10948-bin-ubuntu-x64.tar.gz\"},\n" +
            "    {\"id\": 2, \"name\": \"llama-b10948-bin-macos-arm64.tar.gz\", \"size\": 6789, \"browser_download_url\": \"https://github.com/ggml-org/llama.cpp/releases/download/b10948/llama-b10948-bin-macos-arm64.tar.gz\"}\n" +
            "  ]\n" +
            "}]";

    public void testExtraireTagVersion() {
        Assert.assertEquals("b10948", RuntimeInstaller.extraireTagVersion(JSON_RELEASE_FIXTURE), "Tag de la release");
    }

    public void testExtraireAssetsDepuisJson() {
        List<RuntimeInstaller.RuntimeAsset> assets = RuntimeInstaller.extraireAssetsDepuisJson(JSON_RELEASE_FIXTURE);
        Assert.assertSize(assets, 2, "Deux assets dans la fixture");
        Assert.assertEquals("llama-b10948-bin-ubuntu-x64.tar.gz", assets.get(0).name(), "Nom du premier asset");
        Assert.assertContains(assets.get(0).url(), "ubuntu-x64.tar.gz", "URL du premier asset");
        Assert.assertEquals("llama-b10948-bin-macos-arm64.tar.gz", assets.get(1).name(), "Nom du second asset");
    }

    public void testExtraireAssetsDepuisJsonVideSansCrash() {
        List<RuntimeInstaller.RuntimeAsset> assets = RuntimeInstaller.extraireAssetsDepuisJson("[]");
        Assert.assertSize(assets, 0, "JSON vide renvoie une liste vide, pas une exception");
    }

    // Reproduit le vrai schéma de l'API GitHub : un objet "uploader" imbriqué (avec ses propres
    // accolades) se trouve entre "name" et "browser_download_url" de chaque asset.
    private static final String JSON_RELEASE_AVEC_UPLOADER_IMBRIQUE = "[{\n" +
            "  \"tag_name\": \"b10948\",\n" +
            "  \"name\": \"b10948\",\n" +
            "  \"assets\": [\n" +
            "    {\"url\": \"https://api.github.com/x\", \"id\": 1, \"name\": \"llama-b10948-bin-ubuntu-x64.tar.gz\", \"label\": \"\", " +
            "\"uploader\": {\"login\": \"github-actions[bot]\", \"id\": 41898282, \"url\": \"https://api.github.com/users/x\"}, " +
            "\"content_type\": \"application/gzip\", \"size\": 12345, " +
            "\"browser_download_url\": \"https://github.com/ggml-org/llama.cpp/releases/download/b10948/llama-b10948-bin-ubuntu-x64.tar.gz\"}\n" +
            "  ]\n" +
            "}]";

    public void testExtraireAssetsDepuisJsonAvecUploaderImbrique() {
        List<RuntimeInstaller.RuntimeAsset> assets = RuntimeInstaller.extraireAssetsDepuisJson(JSON_RELEASE_AVEC_UPLOADER_IMBRIQUE);
        Assert.assertSize(assets, 1, "Un seul asset malgré l'objet uploader imbriqué entre les deux champs");
        Assert.assertEquals("llama-b10948-bin-ubuntu-x64.tar.gz", assets.get(0).name(), "Nom correctement isolé");
        Assert.assertContains(assets.get(0).url(), "ubuntu-x64.tar.gz", "URL correctement isolée malgré l'objet imbriqué");
    }

    public void testExtraireTagVersionAvecNomDeReleaseAmbigu() {
        Assert.assertEquals("b10948", RuntimeInstaller.extraireTagVersion(JSON_RELEASE_AVEC_UPLOADER_IMBRIQUE),
                "Le tag_name du release ne doit pas être confondu avec son champ name");
    }

    // --- Extraction ZIP (Windows) avec protection zip-slip ---

    public void testExtraireZipExtraitFichierAttendu() throws Exception {
        Path tempDir = Files.createTempDirectory("zip_src_test");
        Path destDir = Files.createTempDirectory("zip_dest_test");
        try {
            Path zipFile = tempDir.resolve("archive.zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                zos.putNextEntry(new ZipEntry("llama-b10948/llama-cli.exe"));
                zos.write("FAUX_BINAIRE".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("llama-b10948/lib.dll"));
                zos.write("FAUSSE_LIB".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            RuntimeInstaller.extraireZip(zipFile, destDir);

            Path extrait = destDir.resolve("llama-b10948/llama-cli.exe");
            Assert.assertTrue(Files.exists(extrait), "Le binaire doit être extrait avec son arborescence");
            Assert.assertEquals("FAUX_BINAIRE", Files.readString(extrait), "Contenu correct après extraction");
        } finally {
            deleteRecursive(tempDir);
            deleteRecursive(destDir);
        }
    }

    public void testExtraireZipRejetteZipSlip() throws Exception {
        Path tempDir = Files.createTempDirectory("zip_slip_src");
        Path destDir = Files.createTempDirectory("zip_slip_dest");
        try {
            Path zipFile = tempDir.resolve("evil.zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                zos.putNextEntry(new ZipEntry("../../evil.txt"));
                zos.write("malicious".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            boolean rejected = false;
            try {
                RuntimeInstaller.extraireZip(zipFile, destDir);
            } catch (java.io.IOException e) {
                rejected = true;
            }
            Assert.assertTrue(rejected, "Une entrée d'archive tentant de sortir du dossier cible doit être rejetée");
        } finally {
            deleteRecursive(tempDir);
            deleteRecursive(destDir);
        }
    }

    // --- Extraction TAR.GZ (Linux/macOS) via la commande système tar ---

    public void testExtraireTarGzExtraitFichierAttendu() throws Exception {
        Path tempDir = Files.createTempDirectory("targz_src_test");
        Path destDir = Files.createTempDirectory("targz_dest_test");
        try {
            Path contentDir = tempDir.resolve("llama-b10948");
            Files.createDirectories(contentDir);
            Files.writeString(contentDir.resolve("llama-cli"), "FAUX_BINAIRE_LINUX");
            Files.writeString(contentDir.resolve("libggml.so"), "FAUSSE_LIB");

            Path archive = tempDir.resolve("archive.tar.gz");
            Process tarCreate = new ProcessBuilder("tar", "-czf", archive.toString(), "-C", tempDir.toString(), "llama-b10948")
                    .redirectErrorStream(true).start();
            tarCreate.waitFor();
            Assert.assertTrue(Files.exists(archive), "L'archive de test doit être créée (tar disponible)");

            RuntimeInstaller.extraireTarGz(archive, destDir);

            Path extrait = destDir.resolve("llama-b10948/llama-cli");
            Assert.assertTrue(Files.exists(extrait), "Le binaire doit être extrait avec son arborescence");
            Assert.assertEquals("FAUX_BINAIRE_LINUX", Files.readString(extrait), "Contenu correct après extraction");
        } finally {
            deleteRecursive(tempDir);
            deleteRecursive(destDir);
        }
    }

    // --- Détection d'installation du runtime (recherche récursive du binaire) ---

    public void testIsRuntimeInstalledFauxPuisVraiApresExtraction() throws Exception {
        Path tempRuntimeDir = Files.createTempDirectory("runtime_dir_test");
        try {
            Assert.assertFalse(RuntimeInstaller.isRuntimeInstalled(tempRuntimeDir.toString()), "Rien d'installé au départ");

            String nomBinaireLocal = System.getProperty("os.name", "").toLowerCase().contains("win") ? "llama-cli.exe" : "llama-cli";
            Path sousDossier = tempRuntimeDir.resolve("llama-b10948");
            Files.createDirectories(sousDossier);
            Path binaire = sousDossier.resolve(nomBinaireLocal);
            Files.writeString(binaire, "FAUX_BINAIRE");
            binaire.toFile().setExecutable(true);

            Assert.assertTrue(RuntimeInstaller.isRuntimeInstalled(tempRuntimeDir.toString()), "Doit détecter le binaire même dans un sous-dossier versionné");
            Assert.assertNotNull(RuntimeInstaller.getRuntimeBinaryPath(tempRuntimeDir.toString()), "Le chemin résolu ne doit pas être nul");
        } finally {
            deleteRecursive(tempRuntimeDir);
        }
    }

    public void testGetRuntimeBinaryPathRetourneNullSiDossierAbsent() {
        Path dossierInexistant = Path.of("dossier_qui_n_existe_vraiment_pas_12345");
        Assert.assertNull(RuntimeInstaller.getRuntimeBinaryPath(dossierInexistant.toString()), "Un dossier absent ne doit pas planter, juste renvoyer null");
    }

    private static void deleteRecursive(Path path) throws Exception {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }
}
