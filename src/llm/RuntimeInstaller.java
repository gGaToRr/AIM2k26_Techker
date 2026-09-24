package llm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// Installation du moteur llama.cpp qui execute les modeles GGUF.
//
// Le dossier llama/ n'est pas versionne : sans cette installation, un clone neuf a des
// modeles mais rien pour les faire tourner. Version figee et empreintes SHA-256 connues,
// comme pour les modeles : jamais de "derniere version" non verifiee.
// Le moteur est depose dans llama/llama-<version>/, la ou LocalLlmBackend le cherche.
public final class RuntimeInstaller {

    public static final String VERSION = "b11120";
    private static final String URL_RELEASE =
            "https://github.com/ggml-org/llama.cpp/releases/download/" + VERSION + "/";

    // Build CPU de chaque plateforme : aucune dependance a un pilote graphique
    public record Archive(String nom, String sha256, long taille) {
        public String url() { return URL_RELEASE + nom; }
        public boolean estZip() { return nom.endsWith(".zip"); }
    }

    private static final Map<String, Archive> ARCHIVES = Map.of(
            "linux-x64", new Archive("llama-" + VERSION + "-bin-ubuntu-x64.tar.gz",
                    "cae70da61dc74e3012418f7a4b6750eadc6fd0a8fb03270022d471b675e5b4cd", 16997245L),
            "linux-arm64", new Archive("llama-" + VERSION + "-bin-ubuntu-arm64.tar.gz",
                    "32a3b84113d3439dfde3ba8488b644ff18e5a2847a8556f18e5e7ee65acf30dc", 13588955L),
            "macos-x64", new Archive("llama-" + VERSION + "-bin-macos-x64.tar.gz",
                    "538689fdbb2695a416f4c57baaaa4cf0368774ea22f7180c2ef0e5e1f3eccdfd", 11235287L),
            "macos-arm64", new Archive("llama-" + VERSION + "-bin-macos-arm64.tar.gz",
                    "f34a2df471a1a6cb4fafd5dacd5f63a54fdc43971235df4d05634ec760eff106", 11205150L),
            "windows-x64", new Archive("llama-" + VERSION + "-bin-win-cpu-x64.zip",
                    "bbaf0584954c7ef53c2ccc5675f96b8849ac81ca181f540b862eefef1a09a402", 18558140L),
            "windows-arm64", new Archive("llama-" + VERSION + "-bin-win-cpu-arm64.zip",
                    "bde095ca8437d3b042781910baf79bcd813146bc87c1a7429ec95daaa0da86f0", 12033016L)
    );

    // Couture de test : le telechargement reel passe par ModelInstaller.telechargerFichier
    @FunctionalInterface
    public interface Telechargeur {
        boolean telecharger(String url, Path cible, PrintStream out,
                            ModelInstaller.DownloadProgressListener listener, Predicate<Path> verification);
    }

    private RuntimeInstaller() {}

    // "linux-x64", "macos-arm64", "windows-x64"... ou "inconnu"
    public static String plateforme(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        String osCle = os.contains("linux") ? "linux"
                : os.contains("mac") || os.contains("darwin") ? "macos"
                : os.contains("win") ? "windows" : null;
        String archCle = arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64") ? "x64"
                : arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : null;
        return osCle == null || archCle == null ? "inconnu" : osCle + "-" + archCle;
    }

    public static Optional<Archive> archivePour(String osName, String osArch) {
        return Optional.ofNullable(ARCHIVES.get(plateforme(osName, osArch)));
    }

    // Dossier d'installation, dans la racine llama/ du projet
    public static Path repertoireInstallation(Path racineLlama) {
        return racineLlama.resolve("llama-" + VERSION);
    }

    public static Path executable(Path repertoire, boolean windows) {
        return repertoire.resolve(windows ? "llama-completion.exe" : "llama-completion");
    }

    // Sous Windows, un .exe present suffit : il n'y a pas de droit d'execution a verifier
    private static boolean estInstalle(Path repertoire, boolean windows) {
        Path fichier = executable(repertoire, windows);
        return windows ? Files.isRegularFile(fichier) : Files.isExecutable(fichier);
    }

    public static boolean installer(Path racineLlama, PrintStream out, ModelInstaller.DownloadProgressListener listener) {
        return installer(racineLlama, System.getProperty("os.name"), System.getProperty("os.arch"),
                ModelInstaller::telechargerFichier, out, listener);
    }

    public static boolean installer(Path racineLlama, String osName, String osArch, Telechargeur telechargeur,
                                    PrintStream out, ModelInstaller.DownloadProgressListener listener) {
        Optional<Archive> archive = archivePour(osName, osArch);
        if (archive.isEmpty()) {
            out.println("[!] Plateforme non prise en charge (" + osName + " " + osArch + ") : installez llama.cpp"
                    + " vous-meme et mettez llama-cli dans le PATH.");
            return false;
        }
        boolean windows = plateforme(osName, osArch).startsWith("windows");
        Path destination = repertoireInstallation(racineLlama);
        if (estInstalle(destination, windows)) {
            out.println("[*] Le moteur llama.cpp " + VERSION + " est deja installe (" + destination + ").");
            return true;
        }

        out.println("[v] Telechargement du moteur llama.cpp " + VERSION + "...");
        Path fichierArchive = racineLlama.resolve(archive.get().nom());
        boolean telecharge = telechargeur.telecharger(archive.get().url(), fichierArchive, out, listener,
                fichier -> ModelInstaller.verifierIntegrite(fichier, archive.get().sha256(), archive.get().taille(), out));
        if (!telecharge) {
            return false;
        }

        try {
            out.println("[v] Extraction dans " + destination + "...");
            viderRepertoire(destination);
            Files.createDirectories(destination);
            if (archive.get().estZip()) {
                extraireZip(fichierArchive, destination);
            } else {
                extraireTarGz(fichierArchive, destination);
            }
        } catch (Exception e) {
            out.println("[!] Extraction du moteur impossible : " + e.getMessage());
            return false;
        } finally {
            try {
                Files.deleteIfExists(fichierArchive);
            } catch (IOException ignoree) {
                // archive laissee sur le disque : sans consequence, elle sera ecrasee
            }
        }

        if (!estInstalle(destination, windows)) {
            out.println("[!] Moteur extrait, mais llama-completion est introuvable dans " + destination + ".");
            return false;
        }
        out.println("[+] Moteur llama.cpp " + VERSION + " installe.");
        return true;
    }

    // Archives Linux / macOS : un dossier llama-<version>/ a la racine, retire a l'extraction.
    // tar (present sur ces systemes) garde les droits d'execution et les liens des bibliotheques.
    static void extraireTarGz(Path archive, Path destination) throws IOException, InterruptedException {
        Process tar = new ProcessBuilder("tar", "-xzf", archive.toAbsolutePath().toString(),
                "-C", destination.toAbsolutePath().toString(), "--strip-components=1")
                .redirectErrorStream(true).start();
        String sortie = new String(tar.getInputStream().readAllBytes()).trim();
        if (tar.waitFor() != 0) {
            throw new IOException("tar a echoue" + (sortie.isEmpty() ? "" : " : " + sortie));
        }
    }

    // Archives Windows : fichiers a plat. Chaque entree doit rester dans la destination.
    static void extraireZip(Path archive, Path destination) throws IOException {
        Path racine = destination.toAbsolutePath().normalize();
        try (InputStream flux = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(flux)) {
            ZipEntry entree;
            while ((entree = zip.getNextEntry()) != null) {
                Path cible = racine.resolve(entree.getName()).normalize();
                if (!cible.startsWith(racine)) {
                    throw new IOException("entree hors du dossier d'installation : " + entree.getName());
                }
                if (entree.isDirectory()) {
                    Files.createDirectories(cible);
                } else {
                    Files.createDirectories(cible.getParent());
                    Files.copy(zip, cible, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    // Installation precedente incomplete : repartir d'un dossier vide
    private static void viderRepertoire(Path repertoire) throws IOException {
        if (!Files.exists(repertoire)) return;
        try (var fichiers = Files.walk(repertoire)) {
            for (Path fichier : fichiers.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(fichier);
            }
        }
    }
}
