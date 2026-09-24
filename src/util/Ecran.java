package util;

import java.io.PrintStream;

// Mise en page console commune : chaque etape du parcours interactif
// s'ouvre sur le meme bandeau, pour que les blocs restent lisibles.
public final class Ecran {

    private static final String LIGNE = "============================================";
    private static final String VERT = "\u001B[1;32m";
    private static final String RESET = "\u001B[0m";

    private Ecran() {}

    public static void etape(PrintStream out, String titre) {
        String debut = couleurActive(out) ? VERT : "";
        String fin = couleurActive(out) ? RESET : "";
        out.println();
        out.println();
        out.println(debut + LIGNE);
        out.println("  >> " + titre);
        out.println(LIGNE + fin);
        out.println();
    }

    // Couleur seulement vers un vrai terminal : pas de codes ANSI dans un pipe,
    // un fichier ou un flux de test. NO_COLOR (no-color.org) desactive tout.
    private static boolean couleurActive(PrintStream out) {
        return out == System.out && System.console() != null && System.getenv("NO_COLOR") == null;
    }
}
