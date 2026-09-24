package corpus;

import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.ThemeClassifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Recherche des prompts du corpus les plus proches de celui de l'utilisateur, en deux temps :
//
//   1. Themes : les themes proches du prompt (nlp.ThemeClassifier) designent les seuls fichiers
//      de la base a lire (corpus/themes/<langue>/<theme>.json, voir Corpus) :
//      quelques milliers de prompts au lieu de 470 000.
//   2. Mots : parmi eux, ceux qui contiennent le plus de mots de l'utilisateur (filtrage
//      progressif, voir RechercheProgressive), classes par note humaine, score NLP et meme
//      categorie de demande.
//
//   java -cp "bin:src/lib/*" corpus.RechercheThematique "quelles croquettes pour mon chaton" [nombre]
public final class RechercheThematique {

    // couvertures : part des mots de l'utilisateur contenus dans chaque prompt de meilleurs
    // richesse : nombre de mots porteurs de sens du prompt de l'utilisateur
    public record Resultat(String langue, List<String> themes, int prompts, List<RechercheProgressive.Etape> etapes,
                           List<ConstruireIndex.Entree> meilleurs, List<Double> couvertures, int candidats, int richesse,
                           long millisecondes) {}

    // Poids de la proximite des mots selon la richesse lexicale du prompt : un prompt riche
    // (beaucoup de mots precis) est classe surtout par ses mots ; un prompt pauvre (2 ou 3 mots)
    // filtre peu, le theme, le type de demande et la qualite du prompt trouve pesent davantage.
    public static double poidsDesMots(int richesse) {
        return Math.round(Math.max(0.5, Math.min(0.85, 0.4 + 0.1 * richesse)) * 100) / 100.0;
    }

    private final Path dossier;
    // Fichiers de theme deja lus (langue/theme) : une recherche suivante sur le meme theme est immediate
    private final Map<String, List<ConstruireIndex.Entree>> cache = new ConcurrentHashMap<>();

    public RechercheThematique(Path dossier) {
        this.dossier = dossier;
    }

    public Resultat rechercher(String promptUtilisateur, int nombre) {
        return rechercher(promptUtilisateur, nombre, e -> true);
    }

    // filtre : prompts de la base admis comme candidats
    public Resultat rechercher(String promptUtilisateur, int nombre, java.util.function.Predicate<ConstruireIndex.Entree> filtre) {
        long debut = System.currentTimeMillis();
        PromptProfile profil = Lemmatizer.analyser(promptUtilisateur);
        String langue = ThemeClassifier.code(profil.language());
        List<String> themes = ThemeClassifier.themesProches(promptUtilisateur, langue);

        // 1. Seuls les fichiers des themes proches sont lus ; un prompt range dans plusieurs
        //    de ces themes n'est garde qu'une fois
        Map<String, ConstruireIndex.Entree> parId = new LinkedHashMap<>();
        for (String theme : themes) {
            for (ConstruireIndex.Entree e : charger(langue, theme)) {
                if (filtre.test(e)) parId.putIfAbsent(e.prompt().id(), e);
            }
        }
        List<ConstruireIndex.Entree> entrees = new ArrayList<>(parId.values());

        // 2. Filtrage par les mots, sur ce sous-ensemble seulement
        List<Corpus.Prompt> prompts = entrees.stream().map(ConstruireIndex.Entree::prompt).toList();
        RechercheProgressive.Resultat mots = new RechercheProgressive(prompts, false)
                .rechercher(promptUtilisateur, langue, Integer.MAX_VALUE);
        Map<String, ConstruireIndex.Entree> entreeDe = new HashMap<>();
        entrees.forEach(e -> entreeDe.put(e.prompt().id(), e));
        String categorie = String.valueOf(profil.classification().primaryType());
        // Proximite des mots, puis qualite et type de demande, dans des proportions qui
        // dependent de la richesse lexicale du prompt de l'utilisateur
        double poids = poidsDesMots(mots.richesse());
        Map<String, Double> pertinence = new HashMap<>();
        Map<String, Double> couverture = new HashMap<>();
        for (int k = 0; k < mots.meilleurs().size(); k++) {
            pertinence.put(mots.meilleurs().get(k).id(), mots.pertinences().get(k));
            couverture.put(mots.meilleurs().get(k).id(), mots.couvertures().get(k));
        }
        List<ConstruireIndex.Entree> meilleurs = mots.meilleurs().stream().map(p -> entreeDe.get(p.id()))
                .sorted(Comparator.comparingDouble((ConstruireIndex.Entree e) ->
                        poids * pertinence.get(e.prompt().id()) + (1 - poids) * note(e, categorie)).reversed())
                .limit(nombre).toList();
        return new Resultat(langue, themes, entrees.size(), mots.etapes(), meilleurs,
                meilleurs.stream().map(e -> couverture.get(e.prompt().id())).toList(), mots.candidats(), mots.richesse(),
                System.currentTimeMillis() - debut);
    }

    // Entre prompts aussi proches par les mots : note humaine (oasst2) ou score NLP, et un
    // bonus quand la demande est du meme type (concevoir, depanner, rediger...)
    static double note(ConstruireIndex.Entree e, String categorie) {
        double qualite = e.prompt().qualite() != null ? 0.6 * e.prompt().qualite() + 0.4 * e.score() / 100.0
                : e.score() / 100.0;
        return qualite + (categorie.equals(e.categorie()) ? 0.15 : 0.0);
    }

    List<ConstruireIndex.Entree> charger(String langue, String theme) {
        return cache.computeIfAbsent(langue + "/" + theme, cle -> {
            Path fichier = dossier.resolve(langue).resolve(theme + ".json");
            if (!Files.isRegularFile(fichier)) return List.of();
            List<ConstruireIndex.Entree> entrees = new ArrayList<>();
            for (String objet : Corpus.lignesDuTableau(fichier)) entrees.add(ConstruireIndex.lire(objet));
            return entrees;
        });
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage : corpus.RechercheThematique \"prompt\" [nombre]");
            System.exit(1);
        }
        RechercheThematique recherche = new RechercheThematique(ConstruireIndex.DOSSIER);
        Resultat r = recherche.rechercher(args[0], args.length > 1 ? Integer.parseInt(args[1]) : 5);

        System.out.printf("Langue : %s%nThemes proches : %s (%d prompts a examiner)%nRichesse lexicale : %d mots precis "
                        + "(poids des mots dans le classement : %.0f %%)%n%n", r.langue(),
                String.join(", ", r.themes().stream().map(ThemeClassifier::libelle).toList()), r.prompts(), r.richesse(),
                100 * poidsDesMots(r.richesse()));
        for (RechercheProgressive.Etape etape : r.etapes()) {
            System.out.printf("  %-14s %7d -> %6d%s%n", etape.racine(), etape.avant(), etape.apres(),
                    etape.retenue() ? "" : "   (saute : aucun prompt)");
        }
        System.out.printf("%n%d prompts proches, les %d meilleurs (%d ms) :%n", r.candidats(), r.meilleurs().size(),
                r.millisecondes());
        for (ConstruireIndex.Entree e : r.meilleurs()) {
            Corpus.Prompt p = e.prompt();
            String texte = p.texte().replaceAll("\\s+", " ");
            System.out.printf("%n- [%s · %s · score %d%s] %s%n", String.join("+", e.themes()), e.categorie(), e.score(),
                    p.qualite() == null ? "" : String.format(Locale.ROOT, " · qualite %.2f", p.qualite()),
                    texte.length() <= 300 ? texte : texte.substring(0, 300) + "…");
        }
    }
}
