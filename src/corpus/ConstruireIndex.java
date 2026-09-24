package corpus;

import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.ThemeClassifier;
import util.Json;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

// (Re)construit la base de prompts rangee par langue et par theme (voir Corpus) :
//
//   corpus/themes/sommaire.json               themes de chaque langue, nombre de prompts
//   corpus/themes/<langue>/<theme>.json       prompts du theme, les mieux notes d'abord
//
// Chaque prompt est reclasse : themes proches (3 au plus, ou "divers"), categorie (type de
// demande) et score qualite de l'analyse NLP, pour que la recherche n'ait plus a analyser
// les candidats. A relancer apres un nouvel apprentissage des themes (ApprendreThemes).
//
//   java -Xmx14g -cp "bin:src/lib/*" corpus.ConstruireIndex [source]
//
// source : la base elle-meme (par defaut) ou un fichier .jsonl.gz de prompts bruts.
public final class ConstruireIndex {

    public static final Path DOSSIER = Corpus.DOSSIER;
    public static final String SOMMAIRE = "sommaire.json";

    public record Entree(Corpus.Prompt prompt, List<String> themes, String categorie, int score) {}

    private ConstruireIndex() {}

    public static void main(String[] args) throws IOException {
        Path fichier = args.length > 0 ? Path.of(args[0]) : Corpus.DOSSIER;
        long debut = System.currentTimeMillis();
        List<Corpus.Prompt> prompts = Corpus.charger(fichier);
        System.out.printf("%d prompts : analyse et classement par theme...%n", prompts.size());

        List<Entree> entrees = prompts.parallelStream().map(ConstruireIndex::classer).toList();

        // langue -> theme -> entrees
        Map<String, Map<String, List<Entree>>> rangement = new TreeMap<>();
        for (Entree e : entrees) {
            for (String theme : e.themes()) {
                rangement.computeIfAbsent(e.prompt().langue(), l -> new TreeMap<>())
                        .computeIfAbsent(theme, t -> new ArrayList<>()).add(e);
            }
        }
        // Ecrite a cote puis mise en place : la source peut etre la base elle-meme
        Path provisoire = DOSSIER.resolveSibling(DOSSIER.getFileName() + ".nouveau");
        supprimer(provisoire);
        ecrire(provisoire, rangement);
        supprimer(DOSSIER);
        Files.move(provisoire, DOSSIER);
        System.out.printf("Base ecrite dans %s en %.0f s%n", DOSSIER, (System.currentTimeMillis() - debut) / 1000.0);
    }

    static Entree classer(Corpus.Prompt p) {
        PromptProfile profil = Lemmatizer.analyser(p.texte());
        List<String> themes = ThemeClassifier.themesProches(p.texte(), p.langue());
        return new Entree(p, themes, String.valueOf(profil.classification().primaryType()),
                profil.qualityDiagnostic().scoreGlobal());
    }

    // Les meilleurs d'abord : note humaine (oasst2), puis score NLP
    static final Comparator<Entree> MEILLEURS_DABORD = Comparator
            .comparingDouble((Entree e) -> e.prompt().qualite() == null ? -1.0 : e.prompt().qualite()).reversed()
            .thenComparing(Comparator.comparingInt(Entree::score).reversed());

    public static void ecrire(Path dossier, Map<String, Map<String, List<Entree>>> rangement) throws IOException {
        StringBuilder sommaire = new StringBuilder("{\"version\":2,\"sources\":\"OpenAssistant oasst2 (Apache 2.0), "
                + "WildChat-1M par AllenAI (ODC-BY)\",\"langues\":{");
        boolean premiereLangue = true;
        for (Map.Entry<String, Map<String, List<Entree>>> langue : rangement.entrySet()) {
            Files.createDirectories(dossier.resolve(langue.getKey()));
            sommaire.append(premiereLangue ? "" : ",").append(Json.chaine(langue.getKey())).append(":{");
            premiereLangue = false;
            boolean premierTheme = true;
            for (Map.Entry<String, List<Entree>> theme : langue.getValue().entrySet()) {
                String relatif = langue.getKey() + "/" + theme.getKey() + ".json";
                List<Entree> tries = theme.getValue().stream().sorted(MEILLEURS_DABORD).toList();
                // Tableau JSON valide, un objet par ligne : lisible ligne a ligne sans analyseur JSON
                try (Writer sortie = Files.newBufferedWriter(dossier.resolve(relatif), StandardCharsets.UTF_8)) {
                    sortie.write("[\n");
                    for (int i = 0; i < tries.size(); i++) {
                        sortie.write(ligne(tries.get(i)) + (i < tries.size() - 1 ? ",\n" : "\n"));
                    }
                    sortie.write("]\n");
                }
                sommaire.append(premierTheme ? "" : ",").append(Json.chaine(theme.getKey()))
                        .append(":{\"libelle\":").append(Json.chaine(ThemeClassifier.libelle(theme.getKey())))
                        .append(",\"prompts\":").append(tries.size())
                        .append(",\"fichier\":").append(Json.chaine(relatif)).append('}');
                premierTheme = false;
            }
            sommaire.append('}');
        }
        sommaire.append("}}\n");
        Files.writeString(dossier.resolve(SOMMAIRE), sommaire);
    }

    private static void supprimer(Path dossier) throws IOException {
        if (!Files.exists(dossier)) return;
        try (var fichiers = Files.walk(dossier)) {
            for (Path f : fichiers.sorted(Comparator.reverseOrder()).toList()) Files.delete(f);
        }
    }

    static String ligne(Entree e) {
        Corpus.Prompt p = e.prompt();
        return "{\"id\":" + Json.chaine(p.id()) + ",\"source\":" + Json.chaine(p.source())
                + ",\"langue\":" + Json.chaine(p.langue()) + ",\"categorie\":" + Json.chaine(e.categorie())
                + ",\"themes\":" + Json.chaine(String.join(",", e.themes()))
                + ",\"score\":" + e.score()
                + ",\"qualite\":" + (p.qualite() == null ? "null" : String.format(Locale.ROOT, "%.3f", p.qualite()))
                + ",\"auteur\":" + Json.chaine(p.auteur())
                + ",\"texte\":" + Json.chaine(p.texte()) + "}";
    }

    // Relit une ligne de l'index
    public static Entree lire(String ligne) {
        Corpus.Prompt p = Corpus.lire(ligne);
        String themes = Json.extraireChaine(ligne, "themes");
        java.util.regex.Matcher score = java.util.regex.Pattern.compile("\"score\"\\s*:\\s*(\\d+)").matcher(ligne);
        return new Entree(p, themes == null || themes.isEmpty() ? List.of() : List.of(themes.split(",")),
                Json.extraireChaine(ligne, "categorie"), score.find() ? Integer.parseInt(score.group(1)) : 0);
    }
}
