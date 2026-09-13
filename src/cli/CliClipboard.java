package cli;

import java.nio.charset.StandardCharsets;

// Gestionnaire d'accès au presse-papiers multi-plateforme (Linux Wayland/X11, macOS, AWT Fallback)
public class CliClipboard {

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
        } catch (Exception ignored) {
            // Bascule vers le fallback AWT
        }

        // 2. Fallback Java AWT
        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(texte);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // Vérifie si une commande système est disponible dans le PATH
    private static boolean commandeExiste(String cmd) {
        try {
            Process p = new ProcessBuilder("which", cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
