package corpus;

import nlp.AvertissementPrompt;
import nlp.Lemmatizer;
import nlp.PromptProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

// Passe tout le corpus dans l'analyse NLP (quelques millisecondes par prompt) pour trouver
// ce qu'aucun test ecrit a la main ne couvre : plantages, prompts qui bloquent l'analyse,
// langues mal detectees, types de demande jamais choisis, technologies non reconnues.
//
//   java -cp "bin:src/lib/*" corpus.AnalyseCorpus [corpus/themes]
//
// Rapport ecrit dans corpus/rapport_nlp.md.
public final class AnalyseCorpus {

    // Au-dela, l'analyse d'un prompt est consideree comme bloquee
    static final long DELAI_MAX_MS = 5_000;
    private static final int EXEMPLES_PAR_ERREUR = 3;

    // Resume d'une analyse : garder les PromptProfile complets de 470 000 prompts sature la memoire
    private record Resultat(Corpus.Prompt prompt, String langue, String type, String confiance, String domaine,
                            List<String> technologies, int score, long micros) {
        static Resultat de(Corpus.Prompt prompt, PromptProfile profil, long micros) {
            return new Resultat(prompt, profil.language(), String.valueOf(profil.classification().primaryType()),
                    String.valueOf(profil.classification().confidenceLevel()),
                    profil.domainInfo() != null && profil.domainInfo().isDomainIdentified()
                            ? profil.domainInfo().domainName() : "non identifie",
                    List.copyOf(profil.detectedTechnologies()), profil.qualityDiagnostic().scoreGlobal(), micros);
        }
    }

    private AnalyseCorpus() {}

    public static void main(String[] args) throws Exception {
        Path fichier = args.length > 0 ? Path.of(args[0]) : Corpus.DOSSIER;
        long debut = System.currentTimeMillis();
        List<Corpus.Prompt> prompts = Corpus.charger(fichier);
        System.out.printf("%d prompts charges en %.1f s%n", prompts.size(), (System.currentTimeMillis() - debut) / 1000.0);

        Map<String, List<Corpus.Prompt>> erreurs = new ConcurrentHashMap<>();
        List<Corpus.Prompt> bloques = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Resultat> resultats = java.util.Collections.synchronizedList(new ArrayList<>(prompts.size()));
        LongAdder tempsTotal = new LongAdder();
        AtomicInteger faits = new AtomicInteger();

        int coeurs = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(coeurs, tache -> {
            Thread t = new Thread(tache);
            t.setDaemon(true); // une analyse bloquee ne doit pas empecher la fin du programme
            return t;
        });
        debut = System.currentTimeMillis();
        List<Future<?>> taches = new ArrayList<>(prompts.size());
        for (Corpus.Prompt prompt : prompts) {
            taches.add(pool.submit(() -> {
                long t0 = System.nanoTime();
                try {
                    PromptProfile profil = Lemmatizer.analyser(prompt.texte());
                    long micros = (System.nanoTime() - t0) / 1000;
                    tempsTotal.add(micros);
                    resultats.add(Resultat.de(prompt, profil, micros));
                } catch (Throwable e) {
                    String cle = e.getClass().getSimpleName() + " : " + String.valueOf(e.getMessage()).lines().findFirst().orElse("");
                    erreurs.computeIfAbsent(cle, k -> java.util.Collections.synchronizedList(new ArrayList<>())).add(prompt);
                }
                int n = faits.incrementAndGet();
                if (n % 20_000 == 0) System.err.printf("  %d / %d%n", n, prompts.size());
            }));
        }
        for (int i = 0; i < taches.size(); i++) {
            try {
                taches.get(i).get(DELAI_MAX_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                bloques.add(prompts.get(i));
                taches.get(i).cancel(true);
            }
        }
        long duree = System.currentTimeMillis() - debut;

        String rapport = rapport(fichier, prompts.size(), resultats, erreurs, bloques, tempsTotal.sum(), duree, coeurs);
        Path sortie = fichier.resolveSibling("rapport_nlp.md");
        Files.writeString(sortie, rapport);
        System.out.println(rapport);
        System.out.println("Rapport ecrit dans " + sortie);
        System.exit(0);
    }

    private static String rapport(Path fichier, int total, List<Resultat> resultats, Map<String, List<Corpus.Prompt>> erreurs,
                                  List<Corpus.Prompt> bloques, long microsTotal, long dureeMs, int coeurs) {
        StringBuilder r = new StringBuilder("# Analyse NLP du corpus\n\n");
        int nbErreurs = erreurs.values().stream().mapToInt(List::size).sum();
        r.append(String.format(Locale.ROOT, "- Corpus : `%s`, %d prompts%n", fichier, total));
        r.append(String.format(Locale.ROOT, "- Analyses reussies : %d ; plantages : %d ; bloques (> %d ms) : %d%n",
                resultats.size(), nbErreurs, DELAI_MAX_MS, bloques.size()));
        r.append(String.format(Locale.ROOT, "- Duree : %.1f s sur %d coeurs ; %.2f ms par prompt en moyenne%n",
                dureeMs / 1000.0, coeurs, resultats.isEmpty() ? 0.0 : microsTotal / 1000.0 / resultats.size()));

        r.append("\n## Plantages\n\n");
        if (erreurs.isEmpty()) r.append("Aucun.\n");
        erreurs.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.comparingInt(l -> -l.size())))
                .forEach(e -> {
                    r.append("### ").append(e.getKey()).append(" (").append(e.getValue().size()).append(")\n");
                    e.getValue().stream().limit(EXEMPLES_PAR_ERREUR).forEach(p -> r.append("- `").append(p.id())
                            .append("` : ").append(extrait(p.texte())).append('\n'));
                });

        r.append("\n## Prompts qui bloquent l'analyse\n\n");
        if (bloques.isEmpty()) r.append("Aucun.\n");
        bloques.stream().limit(10).forEach(p -> r.append("- `").append(p.id()).append("` (")
                .append(p.texte().length()).append(" car.) : ").append(extrait(p.texte())).append('\n'));

        r.append("\n## Les plus lents\n\n");
        resultats.stream().sorted(Comparator.comparingLong(Resultat::micros).reversed()).limit(5)
                .forEach(x -> r.append(String.format(Locale.ROOT, "- %.0f ms, %d car. : %s%n",
                        x.micros() / 1000.0, x.prompt().texte().length(), extrait(x.prompt().texte()))));

        r.append("\n## Langue detectee selon la langue declaree\n\n| Declaree | Detectee |\n|---|---|\n");
        repartition(resultats, x -> x.prompt().langue()).forEach((langue, n) -> r.append("| ").append(langue).append(" (")
                .append(n).append(") | ").append(pourcentages(resultats.stream()
                        .filter(x -> langue.equals(x.prompt().langue())).map(Resultat::langue).toList()))
                .append(" |\n"));

        r.append("\n## Type de demande\n\n").append(pourcentages(resultats.stream().map(Resultat::type).toList())).append('\n');
        r.append("\n## Confiance de la classification\n\n").append(pourcentages(resultats.stream()
                .map(Resultat::confiance).toList())).append('\n');
        r.append("\n## Domaine identifie\n\n").append(pourcentages(resultats.stream().map(Resultat::domaine).toList()))
                .append('\n');
        r.append("\n## Technologies les plus detectees\n\n").append(pourcentages(resultats.stream()
                .flatMap(x -> x.technologies().stream()).toList(), 20)).append('\n');
        long sansTechno = resultats.stream().filter(x -> x.technologies().isEmpty()).count();
        r.append(String.format(Locale.ROOT, "%nPrompts sans technologie detectee : %.1f %%%n", 100.0 * sansTechno / Math.max(1, resultats.size())));

        r.append("\n## Score qualite\n\n").append(pourcentages(resultats.stream()
                .map(x -> tranche(x.score())).sorted().toList())).append('\n');
        long courts = resultats.stream().filter(x -> AvertissementPrompt.verifier(x.prompt().texte()).isPresent()).count();
        r.append(String.format(Locale.ROOT, "%nPrompts de moins de %d caracteres (avertissement) : %.1f %%%n",
                AvertissementPrompt.LONGUEUR_MINIMALE, 100.0 * courts / Math.max(1, resultats.size())));
        return r.toString();
    }

    private static <T> Map<T, Long> repartition(List<Resultat> resultats, java.util.function.Function<Resultat, T> cle) {
        return resultats.stream().collect(java.util.stream.Collectors.groupingBy(cle, java.util.TreeMap::new,
                java.util.stream.Collectors.counting()));
    }

    private static String pourcentages(List<String> valeurs) {
        return pourcentages(valeurs, Integer.MAX_VALUE);
    }

    private static String pourcentages(List<String> valeurs, int limite) {
        Map<String, Long> compte = valeurs.stream().collect(java.util.stream.Collectors.groupingBy(v -> v,
                java.util.stream.Collectors.counting()));
        return compte.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(limite)
                .map(e -> String.format(Locale.ROOT, "%s %.1f %%", e.getKey(), 100.0 * e.getValue() / Math.max(1, valeurs.size())))
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    private static String tranche(int score) {
        int bas = Math.min(90, score / 10 * 10);
        return String.format("%02d-%02d", bas, bas + 9);
    }

    private static String extrait(String texte) {
        String ligne = texte.replaceAll("\\s+", " ").strip();
        return ligne.length() <= 100 ? ligne : ligne.substring(0, 100) + "…";
    }
}
