package cli;

import gen.MetaPromptEngine;
import nlp.PromptProfile;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

// Format du resultat demande par -o/--output.
// La valeur est soit un mot-cle (txt, md, json), soit un chemin de fichier
// dont l'extension decide du format (.json, .txt, tout le reste en Markdown).
public enum FormatSortie {
    TXT, MD, JSON;

    private static final Pattern TITRE = Pattern.compile("(?m)^#{1,6}\\s+");
    private static final Pattern CLOTURE_CODE = Pattern.compile("(?m)^\\s*```.*\\R?");
    private static final Pattern GRAS = Pattern.compile("(\\*\\*|__)(.+?)\\1");
    private static final Pattern ITALIQUE = Pattern.compile("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])");
    private static final Pattern CODE_EN_LIGNE = Pattern.compile("`([^`\\n]+)`");

    // Sans -o, le prompt reste en Markdown : c'est ce que les templates produisent
    public static FormatSortie depuis(CliArgs options) {
        if (options == null || !options.hasOutput()) return MD;
        String valeur = options.output().trim().toLowerCase(Locale.ROOT);
        return switch (valeur) {
            case "txt", "text", "texte" -> TXT;
            case "md", "markdown" -> MD;
            case "json" -> JSON;
            default -> valeur.endsWith(".json") ? JSON : valeur.endsWith(".txt") ? TXT : MD;
        };
    }

    // Le fichier d'export, si -o designe un chemin plutot qu'un mot-cle de format
    public static Optional<Path> fichierCible(CliArgs options) {
        if (options == null || !options.hasOutput()) return Optional.empty();
        String valeur = options.output().trim();
        return estMotCle(valeur) ? Optional.empty() : Optional.of(Path.of(valeur));
    }

    private static boolean estMotCle(String valeur) {
        return switch (valeur.toLowerCase(Locale.ROOT)) {
            case "txt", "text", "texte", "md", "markdown", "json" -> true;
            default -> false;
        };
    }

    // Met le prompt optimise dans le format demande
    public String rendre(PromptProfile profil, String promptOptimise, CliArgs options) {
        return switch (this) {
            case JSON -> MetaPromptEngine.genererExportJson(profil, promptOptimise, options);
            case TXT -> versTexteBrut(promptOptimise);
            case MD -> promptOptimise;
        };
    }

    // Retire le balisage Markdown en gardant le contenu : le texte des titres,
    // du gras et du code reste, seuls les symboles disparaissent.
    public static String versTexteBrut(String markdown) {
        if (markdown == null) return "";
        String texte = CLOTURE_CODE.matcher(markdown).replaceAll("");
        texte = TITRE.matcher(texte).replaceAll("");
        texte = GRAS.matcher(texte).replaceAll("$2");
        texte = ITALIQUE.matcher(texte).replaceAll("$1");
        texte = CODE_EN_LIGNE.matcher(texte).replaceAll("$1");
        return texte;
    }
}
