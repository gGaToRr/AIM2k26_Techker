package nlp;

import gen.TemplateLoader;
import util.Log;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

// Theme d'un prompt (programmation, cuisine, voyage...), a partir d'un vocabulaire appris
// sur 470 000 vrais prompts.
//
// Deux fichiers dans src/genPrompt/themes/ :
//   - graines.properties : quelques mots-cles surs par theme, ecrits a la main ;
//   - modele.tsv : le vocabulaire de chaque theme, par langue, appris sur le corpus par
//     corpus.ApprendreThemes (poids = sur-representation du mot dans le theme).
// Le score d'un theme est la somme des poids des mots du prompt, plus un bonus par graine.
public final class ThemeClassifier {

    public static final String FICHIER_GRAINES = "themes/graines.properties";
    public static final String FICHIER_MODELE = "themes/modele.tsv";
    public static final String DIVERS = "divers";

    // Une graine trouvee vaut un mot tres caracteristique du theme
    static final double POIDS_GRAINE = 3.0;
    // Score minimal pour affirmer un theme ; en dessous : "divers". Une graine seule suffit.
    public static final double SCORE_MIN = POIDS_GRAINE;
    // Themes proches : ceux qui atteignent cette part du meilleur score
    public static final double PART_DU_MEILLEUR = 0.6;
    public static final int THEMES_MAX = 3;

    public record ScoreTheme(String id, String libelle, double score) {}

    // Une graine : les racines de ses mots (une expression exige toutes ses racines)
    public record Graine(String theme, List<String> racines) {
        public boolean presente(Set<String> racinesPrompt) {
            return !racines.isEmpty() && racinesPrompt.containsAll(racines);
        }
    }

    // Graines d'un theme par langue ("*" : toutes les langues)
    public record Graines(Map<String, String> libelles, Map<String, List<Graine>> parLangue) {
        public List<Graine> pour(String langue) {
            List<Graine> toutes = new ArrayList<>(parLangue.getOrDefault("*", List.of()));
            toutes.addAll(parLangue.getOrDefault(code(langue), List.of()));
            return toutes;
        }
    }

    // langue -> racine -> (theme -> poids)
    public record Modele(Map<String, Map<String, Map<String, Double>>> poids) {
        public static final Modele VIDE = new Modele(Map.of());
    }

    private static volatile Graines graines;
    private static volatile Modele modele;

    private ThemeClassifier() {}

    // Themes du plus au moins probable ; vide si aucun indice. Une technologie reconnue par
    // TechStackDetector (Java, Spring, SQL...) vaut une graine du theme correspondant : un
    // prompt court comme "fix my Spring Boot controller" a peu de mots propres au theme.
    public static List<ScoreTheme> scorer(String texte, String langue) {
        Map<String, Double> bonus = new HashMap<>();
        for (String techno : TechStackDetector.detecterTechnologies(texte)) {
            bonus.merge(techno.startsWith("HTML") ? "web" : "programmation", POIDS_GRAINE, Math::max);
        }
        return scorer(Racines.de(texte), langue, graines(), modele(), bonus);
    }

    public static List<ScoreTheme> scorer(Set<String> racinesPrompt, String langue, Graines graines, Modele modele) {
        return scorer(racinesPrompt, langue, graines, modele, Map.of());
    }

    public static List<ScoreTheme> scorer(Set<String> racinesPrompt, String langue, Graines graines, Modele modele,
                                          Map<String, Double> bonus) {
        Map<String, Double> scores = new HashMap<>(bonus);
        Map<String, Map<String, Double>> vocabulaire = modele.poids().getOrDefault(code(langue), Map.of());
        for (String racine : racinesPrompt) {
            Map<String, Double> parTheme = vocabulaire.get(racine);
            if (parTheme != null) parTheme.forEach((theme, poids) -> scores.merge(theme, poids, Double::sum));
        }
        for (Graine graine : graines.pour(langue)) {
            if (graine.presente(racinesPrompt)) scores.merge(graine.theme(), POIDS_GRAINE, Double::sum);
        }
        return scores.entrySet().stream()
                .map(e -> new ScoreTheme(e.getKey(), graines.libelles().getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(ScoreTheme::score).reversed())
                .toList();
    }

    // Les themes les plus proches (THEMES_MAX au plus), ou "divers" si aucun ne se detache
    public static List<String> themesProches(String texte, String langue) {
        return themesProches(scorer(texte, langue));
    }

    public static List<String> themesProches(List<ScoreTheme> scores) {
        if (scores.isEmpty() || scores.get(0).score() < SCORE_MIN) return List.of(DIVERS);
        double seuil = scores.get(0).score() * PART_DU_MEILLEUR;
        return scores.stream().filter(s -> s.score() >= seuil).limit(THEMES_MAX).map(ScoreTheme::id).toList();
    }

    public static String libelle(String theme) {
        return DIVERS.equals(theme) ? "Divers" : graines().libelles().getOrDefault(theme, theme);
    }

    public static Collection<String> themes() {
        return graines().libelles().keySet();
    }

    // "FR" -> "fr"
    public static String code(String langue) {
        return langue == null ? "" : langue.toLowerCase(Locale.ROOT);
    }

    // --- Chargement ---

    public static Graines graines() {
        if (graines == null) {
            synchronized (ThemeClassifier.class) {
                if (graines == null) {
                    graines = TemplateLoader.resoudreCheminTemplate(FICHIER_GRAINES).map(ThemeClassifier::lireGraines)
                            .orElseGet(() -> {
                                Log.avertir(FICHIER_GRAINES + " introuvable : aucun theme detecte.");
                                return new Graines(Map.of(), Map.of());
                            });
                }
            }
        }
        return graines;
    }

    public static Modele modele() {
        if (modele == null) {
            synchronized (ThemeClassifier.class) {
                if (modele == null) {
                    modele = TemplateLoader.resoudreCheminTemplate(FICHIER_MODELE).map(ThemeClassifier::lireModele)
                            .orElse(Modele.VIDE);
                }
            }
        }
        return modele;
    }

    public static Graines lireGraines(Path fichier) {
        Properties proprietes = new Properties();
        try (Reader lecteur = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            proprietes.load(lecteur);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture de " + fichier + " impossible", e);
        }
        Map<String, String> libelles = new LinkedHashMap<>();
        Map<String, List<Graine>> parLangue = new HashMap<>();
        for (String cle : proprietes.stringPropertyNames().stream().sorted().toList()) {
            String[] parties = cle.split("\\.");
            String theme = parties[0];
            if (parties.length == 2 && parties[1].equals("libelle")) {
                libelles.put(theme, proprietes.getProperty(cle).trim());
            } else if (parties.length >= 2 && parties[1].equals("mots")) {
                String langue = parties.length == 3 ? parties[2] : "*";
                for (String expression : proprietes.getProperty(cle).split(",")) {
                    List<String> racines = new ArrayList<>(Racines.de(expression));
                    if (!racines.isEmpty()) {
                        parLangue.computeIfAbsent(langue, l -> new ArrayList<>()).add(new Graine(theme, racines));
                    }
                }
            }
        }
        // Ordre du fichier pour les libelles : Properties ne le garde pas, on trie par theme
        Map<String, String> ordonnes = new LinkedHashMap<>();
        libelles.keySet().stream().sorted().forEach(t -> ordonnes.put(t, libelles.get(t)));
        return new Graines(ordonnes, parLangue);
    }

    // Lignes "langue<TAB>theme<TAB>racine<TAB>poids" ; "#" : commentaire
    public static Modele lireModele(Path fichier) {
        Map<String, Map<String, Map<String, Double>>> poids = new HashMap<>();
        try {
            for (String ligne : Files.readAllLines(fichier, StandardCharsets.UTF_8)) {
                if (ligne.isBlank() || ligne.startsWith("#")) continue;
                String[] c = ligne.split("\t");
                if (c.length != 4) continue;
                poids.computeIfAbsent(c[0], l -> new HashMap<>()).computeIfAbsent(c[2], r -> new HashMap<>())
                        .put(c[1], Double.parseDouble(c[3]));
            }
        } catch (IOException | NumberFormatException e) {
            Log.erreur("Lecture de " + fichier + " impossible, themes appris ignores", e);
            return Modele.VIDE;
        }
        return new Modele(poids);
    }

    // Pour les tests et l'apprentissage : remplace les fichiers charges
    public static void utiliser(Graines nouvellesGraines, Modele nouveauModele) {
        graines = nouvellesGraines;
        modele = nouveauModele;
    }

    public static Optional<ScoreTheme> meilleur(List<ScoreTheme> scores) {
        return scores.isEmpty() ? Optional.empty() : Optional.of(scores.get(0));
    }

    static List<String> separer(String valeur) {
        return Arrays.stream(valeur.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
