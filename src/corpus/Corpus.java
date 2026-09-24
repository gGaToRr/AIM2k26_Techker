package corpus;

import util.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

// Base de vrais prompts (oasst2 et WildChat, voir doc/corpus/README.md), rangee par theme :
//
//   corpus/themes/sommaire.json               themes de chaque langue et nombre de prompts
//   corpus/themes/<langue>/<theme>.json       tableau JSON, un prompt par ligne
//
// Du JSON simple, lu sans aucune bibliotheque. Un prompt proche de plusieurs themes figure
// dans chacun d'eux : charger() ne le garde qu'une fois.
public final class Corpus {

    public static final Path DOSSIER = Path.of("corpus", "themes");

    // qualite : note donnee par des humains (oasst2, 0 a 1), null si inconnue.
    // auteur : identifiant anonyme de l'utilisateur (null si inconnu)
    public record Prompt(String id, String source, String langue, String texte, Double qualite, String auteur) {
        public Prompt(String id, String source, String langue, String texte, Double qualite) {
            this(id, source, langue, texte, qualite, null);
        }
    }

    private static final Pattern QUALITE = Pattern.compile("\"qualite\"\\s*:\\s*([0-9.eE+-]+)");

    private Corpus() {}

    // Toute la base (dossier de themes), ou un fichier JSON Lines compresse (.jsonl.gz)
    public static List<Prompt> charger(Path source) {
        if (!Files.isDirectory(source)) return chargerJsonl(source);
        java.util.Map<String, Prompt> parId = new java.util.LinkedHashMap<>();
        try (var fichiers = Files.walk(source)) {
            for (Path fichier : fichiers.filter(f -> f.toString().endsWith(".json")
                    && !f.getFileName().toString().equals("sommaire.json")).sorted().toList()) {
                for (String ligne : lignesDuTableau(fichier)) {
                    Prompt p = lire(ligne);
                    parId.putIfAbsent(p.id(), p);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture de la base " + source + " impossible", e);
        }
        return new ArrayList<>(parId.values());
    }

    // Objets d'un fichier de theme : "[", puis un objet par ligne (suivi d'une virgule), puis "]"
    public static List<String> lignesDuTableau(Path fichier) {
        List<String> objets = new ArrayList<>();
        try (BufferedReader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                String objet = ligne.strip();
                if (objet.endsWith(",")) objet = objet.substring(0, objet.length() - 1);
                if (objet.startsWith("{")) objets.add(objet);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture de " + fichier + " impossible", e);
        }
        return objets;
    }

    private static List<Prompt> chargerJsonl(Path fichier) {
        List<Prompt> prompts = new ArrayList<>();
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(fichier), 1 << 16), StandardCharsets.UTF_8))) {
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                if (!ligne.isBlank()) prompts.add(lire(ligne));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du corpus " + fichier + " impossible", e);
        }
        return prompts;
    }

    public static Prompt lire(String ligne) {
        Matcher qualite = QUALITE.matcher(ligne);
        return new Prompt(Json.extraireChaine(ligne, "id"), Json.extraireChaine(ligne, "source"),
                Json.extraireChaine(ligne, "langue"), Json.extraireChaine(ligne, "texte"),
                qualite.find() ? Double.valueOf(qualite.group(1)) : null, Json.extraireChaine(ligne, "auteur"));
    }
}
