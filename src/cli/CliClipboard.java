package cli;

import java.nio.charset.StandardCharsets;
import util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Gestionnaire d'accès au presse-papiers multi-plateforme (Linux Wayland/X11, macOS, AWT Fallback)
public class CliClipboard {

    // Le PATH ne change pas pendant la duree du process : une commande absente le reste,
    // une commande presente aussi. Le resultat de "which" est donc memorise par nom.
    private static final Map<String, Boolean> CACHE_COMMANDES = new ConcurrentHashMap<>();

    // Nombre de process "which" reellement lances depuis le demarrage (observabilite et tests)
    private static final java.util.concurrent.atomic.AtomicInteger SONDAGES_SYSTEME =
            new java.util.concurrent.atomic.AtomicInteger();

    // Copie la chaîne passée en paramètre dans le presse-papiers
    public static boolean copierTexte(String texte) {
        if (texte == null || texte.isBlank()) {
            return false;
        }

        // 1. Essai avec les utilitaires natifs du système d'exploitation
        try {
            ProcessBuilder pb = null;
            if (System.getenv("WAYLAND_DISPLAY") != null && commandeExiste("wl-copy")) {
                pb = new ProcessBuilder("wl-copy");
            } else if (commandeExiste("xclip")) {
                pb = new ProcessBuilder("xclip", "-selection", "clipboard");
            } else if (commandeExiste("xsel")) {
                pb = new ProcessBuilder("xsel", "--clipboard", "--input");
            } else if (commandeExiste("pbcopy")) {
                pb = new ProcessBuilder("pbcopy");
            }

            if (pb != null) {
                Process p = pb.start();
                try (var os = p.getOutputStream()) {
                    os.write(texte.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Bascule vers le fallback AWT
            Log.exceptionIgnoree("Copie via l'utilitaire systeme, bascule sur le fallback AWT", e);
        }

        // 2. Fallback Java AWT
        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(texte);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            return true;
        } catch (Throwable t) {
            Log.exceptionIgnoree("Copie via le presse-papiers AWT (environnement sans affichage ?)", t);
            return false;
        }
    }

    // Vérifie si une commande système est disponible dans le PATH.
    // Jusqu'a 4 appels par copie : sans cache, autant de process "which" relances a chaque fois.
    public static boolean commandeExiste(String cmd) {
        if (cmd == null || cmd.isBlank()) {
            return false;
        }
        return CACHE_COMMANDES.computeIfAbsent(cmd, CliClipboard::detecterCommande);
    }

    private static boolean detecterCommande(String cmd) {
        SONDAGES_SYSTEME.incrementAndGet();
        try {
            Process p = new ProcessBuilder("which", cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            Log.exceptionIgnoree("Sondage de la commande " + cmd, e);
            return false;
        }
    }

    // Nombre de process "which" reellement lances : doit rester borne par le nombre
    // de commandes distinctes sondees, quel que soit le nombre de copies effectuees.
    public static int nombreDeSondagesSysteme() {
        return SONDAGES_SYSTEME.get();
    }

    // Reinitialise cache et compteur (tests uniquement)
    public static void viderCacheCommandes() {
        CACHE_COMMANDES.clear();
        SONDAGES_SYSTEME.set(0);
    }
}
