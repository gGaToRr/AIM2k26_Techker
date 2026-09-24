package corpus;

import gen.TemplateLoader;
import nlp.Racines;
import nlp.ThemeClassifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

// Apprend le vocabulaire de chaque theme sur le corpus, a partir des graines ecrites a la
// main (src/genPrompt/themes/graines.properties). Ecrit src/genPrompt/themes/modele.tsv.
//
//   java -Xmx12g -cp "bin:src/lib/*" corpus.ApprendreThemes [corpus/themes]
//
// 1. Exemples : prompts qui contiennent les graines d'un seul theme (le plus de graines, sans
//    egalite). Les prompts tres longs melangent les sujets : ils ne servent pas d'exemples.
// 2. Vocabulaire : pour chaque theme et chaque langue, les racines sur-representees dans ses
//    exemples par rapport aux exemples des autres themes (log du rapport des frequences).
// 3. Une seconde passe reclasse tout le corpus avec ce vocabulaire et apprend de nouveau :
//    le theme gagne les prompts qui n'ont aucune graine mais son vocabulaire.
// Rapport de relecture (vocabulaire appris par theme) : corpus/themes_apprentissage.md.
public final class ApprendreThemes {

    static final int LONGUEUR_MAX_EXEMPLE = 2000;
    // Un mot doit etre employe par au moins autant d'auteurs du theme pour etre appris, et par
    // au moins cette part d'entre eux
    static final int PRESENCE_MIN = 5;
    static final double PART_MIN = 0.005;
    // Plafond de cette exigence : sur un grand theme (15 000 auteurs en programmation), 0,5 %
    // ecarterait des mots utiles comme "controller"
    static final int PRESENCE_PLAFOND = 20;
    // Au-dela, la sur-representation n'ajoute rien : le classement des mots privilegie alors
    // leur frequence (sinon les mots rarissimes d'un seul prompt passent devant)
    static final double POIDS_PLAFOND = 4.0;
    // Les langues apprises s'ecrivent en alphabet latin : un mot russe ou chinois dans un
    // prompt marque "fr" (demande de traduction) n'est pas du vocabulaire francais
    private static final java.util.regex.Pattern LATIN = java.util.regex.Pattern.compile("[a-z0-9]+");
    // Sur-representation minimale (log) : e^1 = 2,7 fois plus frequent que dans les autres themes
    static final double POIDS_MIN = 1.0;
    static final int MOTS_PAR_THEME = 1000;
    private static final List<String> LANGUES = List.of("fr", "en", "es", "de");

    private ApprendreThemes() {}

    public static void main(String[] args) throws IOException {
        Path fichier = args.length > 0 ? Path.of(args[0]) : Corpus.DOSSIER;
        List<Corpus.Prompt> prompts = Corpus.charger(fichier);
        ThemeClassifier.Graines graines = ThemeClassifier.graines();
        System.out.printf("%d prompts, %d themes%n", prompts.size(), graines.libelles().size());

        List<Set<String>> racines = prompts.parallelStream().map(p -> Racines.de(p.texte())).toList();
        StringBuilder rapport = new StringBuilder("# Apprentissage des themes\n");
        Map<String, Map<String, Map<String, Double>>> poids = new TreeMap<>();

        for (String langue : LANGUES) {
            int[] membres = IntStream.range(0, prompts.size()).filter(i -> langue.equals(prompts.get(i).langue())
                    && prompts.get(i).texte().length() <= LONGUEUR_MAX_EXEMPLE).toArray();
            List<ThemeClassifier.Graine> grainesLangue = graines.pour(langue);

            // 1. Exemples designes par les graines
            String[] etiquettes = new String[membres.length];
            IntStream.range(0, membres.length).parallel().forEach(k -> etiquettes[k] =
                    etiquetteParGraines(racines.get(membres[k]), grainesLangue));
            // Auteur inconnu : chaque prompt compte pour un auteur distinct
            String[] auteurs = new String[membres.length];
            for (int k = 0; k < membres.length; k++) {
                Corpus.Prompt p = prompts.get(membres[k]);
                auteurs[k] = p.auteur() != null ? p.auteur() : p.id();
            }
            Map<String, Map<String, Double>> vocabulaire = apprendre(membres, etiquettes, racines, auteurs);
            int parGraines = compter(etiquettes);

            // 2. Seconde passe : reclassement avec le vocabulaire appris
            ThemeClassifier.Modele provisoire = new ThemeClassifier.Modele(Map.of(langue, vocabulaire));
            IntStream.range(0, membres.length).parallel().forEach(k -> {
                List<String> themes = ThemeClassifier.themesProches(
                        ThemeClassifier.scorer(racines.get(membres[k]), langue, graines, provisoire));
                etiquettes[k] = themes.size() == 1 && !themes.get(0).equals(ThemeClassifier.DIVERS) ? themes.get(0) : null;
            });
            vocabulaire = apprendre(membres, etiquettes, racines, auteurs);
            poids.put(langue, vocabulaire);

            rapport.append(String.format(Locale.ROOT, "%n## %s : %d prompts%n%nExemples par les graines : %d (%.1f %%) ; "
                    + "apres la seconde passe : %d (%.1f %%)%n%n", langue, membres.length, parGraines,
                    100.0 * parGraines / Math.max(1, membres.length), compter(etiquettes),
                    100.0 * compter(etiquettes) / Math.max(1, membres.length)));
            rapport.append(resume(vocabulaire, etiquettes, graines));
            System.out.printf("%s : %d prompts, %d exemples par les graines, %d apres la seconde passe%n",
                    langue, membres.length, parGraines, compter(etiquettes));
        }

        Path modele = TemplateLoader.resoudreDossierTemplate("themes").orElseThrow()
                .resolve(Path.of(ThemeClassifier.FICHIER_MODELE).getFileName());
        ecrireModele(modele, poids);
        Path sortie = fichier.resolveSibling("themes_apprentissage.md");
        Files.writeString(sortie, rapport);
        System.out.println("Modele ecrit dans " + modele + ", rapport dans " + sortie);
    }

    // Theme ayant le plus de graines presentes, sans egalite ; null sinon
    static String etiquetteParGraines(Set<String> racinesPrompt, List<ThemeClassifier.Graine> graines) {
        Map<String, Integer> compte = new HashMap<>();
        for (ThemeClassifier.Graine g : graines) {
            if (g.presente(racinesPrompt)) compte.merge(g.theme(), 1, Integer::sum);
        }
        int max = compte.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (max == 0) return null;
        List<String> meilleurs = compte.entrySet().stream().filter(e -> e.getValue() == max).map(Map.Entry::getKey).toList();
        return meilleurs.size() == 1 ? meilleurs.get(0) : null;
    }

    // racine -> (theme -> poids) : log((df_theme + 0,5) / (N_theme + 1)) - log((df_autres + 0,5) / (N_autres + 1))
    // Tout est compte en AUTEURS distincts, pas en prompts : quelques utilisateurs envoient des
    // centaines de variantes d'un meme prompt (series de fanfictions), dont les noms de
    // personnages sembleraient sinon caracteristiques du theme.
    static Map<String, Map<String, Double>> apprendre(int[] membres, String[] etiquettes, List<Set<String>> racines,
                                                      String[] auteurs) {
        Map<String, Integer> exemplesParTheme = new HashMap<>(); // theme -> nb d'auteurs
        Map<String, Map<String, Integer>> presence = new HashMap<>(); // racine -> theme -> nb d'auteurs
        Set<String> auteursParTheme = new java.util.HashSet<>();
        Set<Long> dejaCompte = new java.util.HashSet<>(); // (theme, racine, auteur)
        for (int k = 0; k < membres.length; k++) {
            if (etiquettes[k] == null) continue;
            String theme = etiquettes[k];
            if (auteursParTheme.add(theme + "|" + auteurs[k])) exemplesParTheme.merge(theme, 1, Integer::sum);
            for (String racine : racines.get(membres[k])) {
                if (!LATIN.matcher(racine).matches()) continue;
                long cle = 31L * (31L * theme.hashCode() + racine.hashCode()) + ((long) auteurs[k].hashCode() << 32);
                if (dejaCompte.add(cle)) {
                    presence.computeIfAbsent(racine, r -> new HashMap<>()).merge(theme, 1, Integer::sum);
                }
            }
        }
        int total = exemplesParTheme.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, List<Map.Entry<String, Double>>> candidats = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : presence.entrySet()) {
            int partout = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> t : e.getValue().entrySet()) {
                int dansTheme = t.getValue();
                int nTheme = exemplesParTheme.get(t.getKey());
                if (dansTheme < Math.max(PRESENCE_MIN, Math.min(PART_MIN * nTheme, PRESENCE_PLAFOND))) continue;
                double w = Math.log((dansTheme + 0.5) / (nTheme + 1.0))
                        - Math.log((partout - dansTheme + 0.5) / (total - nTheme + 1.0));
                if (w >= POIDS_MIN) candidats.computeIfAbsent(t.getKey(), x -> new ArrayList<>()).add(Map.entry(e.getKey(), w));
            }
        }
        // Pour chaque theme, les mots a la fois caracteristiques et frequents
        Map<String, Map<String, Double>> vocabulaire = new HashMap<>();
        candidats.forEach((theme, liste) -> liste.stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, Double> m) ->
                        Math.min(m.getValue(), POIDS_PLAFOND) * Math.log1p(presence.get(m.getKey()).get(theme))).reversed())
                .limit(MOTS_PAR_THEME)
                .forEach(m -> vocabulaire.computeIfAbsent(m.getKey(), r -> new HashMap<>())
                        .put(theme, Math.round(m.getValue() * 100) / 100.0)));
        return vocabulaire;
    }

    private static int compter(String[] etiquettes) {
        int n = 0;
        for (String e : etiquettes) if (e != null) n++;
        return n;
    }

    private static String resume(Map<String, Map<String, Double>> vocabulaire, String[] etiquettes,
                                 ThemeClassifier.Graines graines) {
        Map<String, Integer> parTheme = new TreeMap<>();
        for (String e : etiquettes) if (e != null) parTheme.merge(e, 1, Integer::sum);
        Map<String, List<Map.Entry<String, Double>>> motsParTheme = new TreeMap<>();
        vocabulaire.forEach((racine, themes) -> themes.forEach((theme, w) ->
                motsParTheme.computeIfAbsent(theme, t -> new ArrayList<>()).add(Map.entry(racine, w))));
        StringBuilder sb = new StringBuilder("| Theme | Prompts | Mots appris | Exemples de mots |\n|---|---|---|---|\n");
        for (String theme : graines.libelles().keySet()) {
            List<Map.Entry<String, Double>> mots = motsParTheme.getOrDefault(theme, List.of());
            sb.append("| ").append(graines.libelles().get(theme)).append(" | ").append(parTheme.getOrDefault(theme, 0))
                    .append(" | ").append(mots.size()).append(" | ")
                    .append(String.join(", ", mots.stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .limit(15).map(Map.Entry::getKey).toList())).append(" |\n");
        }
        return sb.toString();
    }

    private static void ecrireModele(Path fichier, Map<String, Map<String, Map<String, Double>>> poids) throws IOException {
        StringBuilder sb = new StringBuilder("# Vocabulaire des themes appris sur le corpus (corpus.ApprendreThemes) : "
                + "ne pas modifier a la main.\n# langue\ttheme\tracine\tpoids\n");
        poids.forEach((langue, vocabulaire) -> new TreeMap<>(vocabulaire).forEach((racine, themes) ->
                new TreeMap<>(themes).forEach((theme, w) -> sb.append(langue).append('\t').append(theme).append('\t')
                        .append(racine).append('\t').append(String.format(Locale.ROOT, "%.2f", w)).append('\n'))));
        Files.writeString(fichier, sb, StandardCharsets.UTF_8);
    }
}
