package corpus;

import llm.ModelInstaller;
import llm.RuntimeInstaller;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

// Installation de la base de prompts (corpus/themes/), comme un modele : une archive zip
// publiee dans une release GitHub, taille et empreinte SHA-256 verifiees, puis extraite.
// Aucune dependance : telechargement et zip sont dans le JDK.
//
//   java -cp "bin:src/lib/*" Main --corpus-install
public final class InstallationBase {

    public static final String VERSION = "base-prompts-v1";
    public static final String ARCHIVE = "aim2k26-base-prompts-v1.zip";
    public static final String URL = "https://github.com/gGaToRr/AIM2k26_Techker/releases/download/"
            + VERSION + "/" + ARCHIVE;
    // Reference de l'archive publiee : a mettre a jour a chaque nouvelle version de la base
    public static final String SHA256 = "5a75e8c913593b4c585613a97da61d9528aae4b8e8e4508d53221c1c24dbc15e";
    public static final long TAILLE = 138_231_227L;

    // Couture de test : le telechargement reel passe par ModelInstaller.telechargerFichier
    @FunctionalInterface
    public interface Telechargeur {
        boolean telecharger(String url, Path cible, PrintStream out, ModelInstaller.DownloadProgressListener listener,
                            java.util.function.Predicate<Path> verification);
    }

    private InstallationBase() {}

    public static boolean installee() {
        return Files.isRegularFile(Corpus.DOSSIER.resolve(ConstruireIndex.SOMMAIRE));
    }

    public static boolean installer(PrintStream out, ModelInstaller.DownloadProgressListener listener) {
        return installer(Corpus.DOSSIER, URL, SHA256, TAILLE, ModelInstaller::telechargerFichier, out, listener);
    }

    // dossier : la base (corpus/themes) ; l'archive contient le dossier "themes/"
    public static boolean installer(Path dossier, String url, String sha256, long taille, Telechargeur telechargeur,
                                    PrintStream out, ModelInstaller.DownloadProgressListener listener) {
        if (Files.isRegularFile(dossier.resolve(ConstruireIndex.SOMMAIRE))) {
            out.println("[*] La base de prompts est deja installee (" + dossier + ").");
            return true;
        }
        Path parent = dossier.toAbsolutePath().getParent();
        Path archive = parent.resolve(ARCHIVE);
        out.println("[v] Telechargement de la base de prompts (" + VERSION + ")...");
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            out.println("[!] Dossier " + parent + " impossible a creer : " + e.getMessage());
            return false;
        }
        boolean telecharge = telechargeur.telecharger(url, archive, out, listener,
                fichier -> ModelInstaller.verifierIntegrite(fichier, sha256, taille, out));
        if (!telecharge) return false;
        try {
            out.println("[v] Extraction dans " + dossier + "...");
            supprimer(dossier);
            RuntimeInstaller.extraireZip(archive, parent);
        } catch (IOException e) {
            out.println("[!] Extraction de la base impossible : " + e.getMessage());
            return false;
        } finally {
            try {
                Files.deleteIfExists(archive);
            } catch (IOException ignoree) {
                // archive laissee sur le disque : ecrasee au prochain essai
            }
        }
        if (!Files.isRegularFile(dossier.resolve(ConstruireIndex.SOMMAIRE))) {
            out.println("[!] Base extraite, mais " + ConstruireIndex.SOMMAIRE + " est introuvable.");
            return false;
        }
        out.println("[+] Base de prompts installee.");
        return true;
    }

    private static void supprimer(Path dossier) throws IOException {
        if (!Files.exists(dossier)) return;
        try (var fichiers = Files.walk(dossier)) {
            for (Path f : fichiers.sorted(Comparator.reverseOrder()).toList()) Files.delete(f);
        }
    }
}
