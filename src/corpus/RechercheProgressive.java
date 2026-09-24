package corpus;

import nlp.Lemmatizer;
import nlp.PromptProfile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Recherche des meilleurs prompts du corpus proches de celui de l'utilisateur, par
// filtrage progressif sur les mots :
//
//   "je veux des croquettes pour mon chaton"
//   croquette  ->  37 prompts parlent de croquettes
//   chat       ->  12 parmi eux parlent aussi de chats (chat, chats, chaton...)
//   ...           et ainsi de suite, tant qu'il reste des prompts
//
// Les meilleurs candidats contiennent le plus grand nombre de mots de l'utilisateur : la
// plus longue combinaison de mots qui donne encore des resultats. Un ordre fixe de filtrage
// echoue : du plus courant au plus rare, "mois" ou "donner" reduisent d'abord la liste a
// des prompts sans rapport ; du plus rare au plus courant, un mot parasite ("velo") ecarte
// le vrai sujet. Les etapes affichees vont du mot le plus precis au plus courant, les mots
// qui auraient vide la liste sont sautes. Un mot trop courant (> FREQUENCE_MAX du corpus)
// ne dit rien du sujet et n'est pas utilise. Les prompts restants sont classes du
// meilleur au moins bon : note de qualite humaine (oasst2), puis score qualite NLP.
public final class RechercheProgressive {

    private static final int RACINE_MIN = nlp.Racines.LONGUEUR_MIN;

    // Mots de la demande, pas du sujet : pronoms, verbes de requete, formules de politesse.
    // "donne moi une recette de lasagnes" : seuls "recette" et "lasagnes" disent le sujet ;
    // sinon "donne moi une recette de panini" passerait pour le prompt le plus proche.
    static final Set<String> MOTS_GENERIQUES = java.util.stream.Stream.of(
            // francais
            "moi", "toi", "lui", "leur", "nous", "vous", "elle", "elles", "eux", "mon", "ton", "son", "notre", "votre",
            "donne", "donner", "donnez", "fais", "faire", "faites", "ecris", "ecrire", "ecrivez", "redige", "rediger",
            "redigez", "aide", "aider", "aidez", "peux", "pourrais", "voudrais", "veux", "vouloir", "besoin", "stp",
            "svp", "merci", "bonjour", "salut", "explique", "expliquer", "dis", "dire", "cree", "creer", "propose",
            "proposer", "trouve", "trouver", "comment", "quoi", "quel", "quelle", "quels", "quelles", "pourquoi",
            // anglais
            "please", "give", "make", "write", "help", "want", "need", "can", "could", "would", "tell", "create",
            "explain", "show", "provide", "hello", "thanks", "thank", "what", "how", "why", "which",
            // espagnol, allemand
            "dame", "hazme", "escribe", "ayudame", "puedes", "quiero", "necesito", "por", "favor", "hola",
            "gib", "schreibe", "hilf", "kannst", "bitte", "mochte", "brauche", "hallo")
            .map(nlp.Racines::racine).collect(java.util.stream.Collectors.toUnmodifiableSet());

    // Un mot present dans plus de cette part du corpus ne dit rien du sujet ("write", "mois").
    // Mesure sans valeur sur un petit corpus : appliquee a partir de CORPUS_MIN_FREQUENCE prompts.
    static final double FREQUENCE_MAX = 0.05;
    static final int CORPUS_MIN_FREQUENCE = 1000;

    public record Etape(String racine, int avant, int apres, boolean retenue) {}

    // pertinences : proximite de chaque prompt de meilleurs, de 0 a 1 (1 = le plus proche)
    // couvertures : part des mots de l'utilisateur que chacun contient, de 0 a 1
    // richesse : nombre de mots porteurs de sens du prompt de l'utilisateur (richesse lexicale)
    public record Resultat(List<Etape> etapes, List<Corpus.Prompt> meilleurs, int candidats, List<Double> pertinences,
                           List<Double> couvertures, int richesse) {}

    // BM25, la formule des moteurs de recherche : chaque mot pese selon sa rarete, et un prompt
    // long, qui contient beaucoup de mots par hasard, compte un peu moins
    static final double BM25_K = 1.2;
    static final double BM25_B = 0.75;
    // Candidats : les prompts d'au moins cette part de la pertinence du meilleur
    static final double PERTINENCE_MIN = 0.5;
    // Part de la proximite dans le classement final (le reste : qualite du prompt)
    static final double POIDS_PERTINENCE = 0.7;

    private final List<Corpus.Prompt> prompts;
    // langue -> (racine -> indices croissants des prompts qui la contiennent). Construit a la
    // premiere recherche dans une langue : les autres langues ne coutent rien.
    private final Map<String, Map<String, int[]>> index = new java.util.concurrent.ConcurrentHashMap<>();

    // classerParNlp : classer les candidats en les analysant (score qualite NLP). Faux quand
    // l'appelant connait deja leurs scores (index par theme) : candidats rendus sans tri.
    private final boolean classerParNlp;

    public RechercheProgressive(List<Corpus.Prompt> prompts) {
        this(prompts, true);
    }

    public RechercheProgressive(List<Corpus.Prompt> prompts, boolean classerParNlp) {
        this.prompts = prompts;
        this.classerParNlp = classerParNlp;
    }

    private Map<String, int[]> indexDe(String langue) {
        return index.computeIfAbsent(langue, this::construireIndex);
    }

    // Racines de chaque prompt calculees en parallele, puis listes d'indices triees
    private Map<String, int[]> construireIndex(String langue) {
        int[] membres = java.util.stream.IntStream.range(0, prompts.size())
                .filter(i -> langue.equals(prompts.get(i).langue())).toArray();
        List<Set<String>> racinesParPrompt = Arrays.stream(membres).parallel()
                .mapToObj(i -> racines(prompts.get(i).texte())).toList();
        Map<String, List<Integer>> parRacine = new HashMap<>();
        for (int k = 0; k < membres.length; k++) {
            for (String racine : racinesParPrompt.get(k)) {
                parRacine.computeIfAbsent(racine, r -> new ArrayList<>()).add(membres[k]);
            }
        }
        Map<String, int[]> compact = new HashMap<>(parRacine.size() * 2);
        parRacine.forEach((racine, liste) -> compact.put(racine, liste.stream().mapToInt(Integer::intValue).toArray()));
        compact.put(TAILLE, new int[] {membres.length});
        // Nombre de mots de chaque prompt (indice global) pour la normalisation de BM25
        int[] longueurs = new int[prompts.size()];
        long somme = 0;
        for (int k = 0; k < membres.length; k++) {
            longueurs[membres[k]] = racinesParPrompt.get(k).size();
            somme += longueurs[membres[k]];
        }
        compact.put(LONGUEURS, longueurs);
        compact.put(MOYENNE, new int[] {(int) Math.max(1, somme / Math.max(1, membres.length))});
        return compact;
    }

    // Entree reservee de l'index : nombre de prompts de la langue (jamais une racine : trop courte)
    private static final String TAILLE = "#";
    private static final String LONGUEURS = "#longueurs";
    private static final String MOYENNE = "#moyenne";

    // Racines distinctes d'un texte, dans l'ordre d'apparition (voir nlp.Racines)
    public static Set<String> racines(String texte) {
        return nlp.Racines.de(texte);
    }

    public static String racine(String mot) {
        return nlp.Racines.racine(mot);
    }

    // langue : "fr", "en"... (la detection NLP renvoie "FR", "EN")
    public Resultat rechercher(String promptUtilisateur, String langue, int nombre) {
        String code = langue == null ? "" : langue.toLowerCase(Locale.ROOT);
        Map<String, int[]> parRacine = indexDe(code);
        int tailleCorpus = parRacine.get(TAILLE)[0];

        // Mots porteurs de sens seulement : l'analyse NLP retire deja les mots vides
        PromptProfile profil = Lemmatizer.analyser(promptUtilisateur);
        // Richesse lexicale : tous les mots precis de l'utilisateur, y compris ceux que la base
        // ne connait pas ("couscous" absent n'en est pas moins un mot du sujet). La couverture
        // d'un prompt se mesure sur eux, pas seulement sur les mots presents dans la base.
        List<String> termes = new ArrayList<>();
        java.util.Set<String> motsPrecis = new java.util.LinkedHashSet<>();
        for (String token : profil.tokens()) {
            String racine = racine(token);
            int[] liste = parRacine.get(racine);
            boolean tropCourant = tailleCorpus >= CORPUS_MIN_FREQUENCE && liste != null
                    && liste.length > FREQUENCE_MAX * tailleCorpus;
            if (racine.length() < RACINE_MIN || tropCourant || MOTS_GENERIQUES.contains(racine)) continue;
            motsPrecis.add(racine);
            if (liste != null && !termes.contains(racine)) termes.add(racine);
        }
        int richesse = motsPrecis.size();
        // Pertinence de chaque prompt (BM25) : somme, sur les mots de l'utilisateur qu'il
        // contient, de la rarete du mot, attenuee pour les prompts longs. Un mot rare
        // ("proprietaire", "fuite") dit plus du sujet qu'un mot courant ("ecrire", "signaler").
        int[] longueurs = parRacine.get(LONGUEURS);
        double moyenne = parRacine.get(MOYENNE)[0];
        Map<Integer, Double> pertinence = new HashMap<>();
        Map<Integer, Integer> motsTrouves = new HashMap<>();
        for (String terme : termes) {
            int presence = parRacine.get(terme).length;
            double rarete = Math.log(1 + (tailleCorpus - presence + 0.5) / (presence + 0.5));
            for (int i : parRacine.get(terme)) {
                double norme = BM25_K * (1 - BM25_B + BM25_B * longueurs[i] / moyenne);
                pertinence.merge(i, rarete * (BM25_K + 1) / (1 + norme), Double::sum);
                motsTrouves.merge(i, 1, Integer::sum);
            }
        }
        // Couverture : part des mots de l'utilisateur presents. Un prompt qui en contient
        // plusieurs passe devant un prompt qui n'en contient qu'un, meme tres rare (un mot
        // parasite ne decide pas seul du sujet)
        pertinence.replaceAll((i, p) -> p * motsTrouves.get(i) / richesse);
        if (pertinence.isEmpty()) {
            List<Etape> etapes = termes.stream().map(t -> new Etape(t, tailleCorpus, 0, false)).toList();
            return new Resultat(etapes, List.of(), 0, List.of(), List.of(), richesse);
        }
        double maximum = pertinence.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        List<Integer> candidats = pertinence.entrySet().stream().filter(e -> e.getValue() >= PERTINENCE_MIN * maximum)
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()).map(Map.Entry::getKey).toList();

        // Etapes affichees : le filtrage progressif qui mene au meilleur candidat, du mot le
        // plus precis au plus courant ; les autres mots sont sautes (ils auraient vide la liste)
        Set<String> motsDuMeilleur = racines(prompts.get(candidats.get(0)).texte());
        List<String> retenus = new ArrayList<>(termes.stream().filter(motsDuMeilleur::contains).toList());
        retenus.sort(Comparator.comparingInt((String r) -> parRacine.get(r).length));
        List<Etape> etapes = new ArrayList<>();
        int[] courant = null;
        for (String terme : retenus) {
            int[] suivant = courant == null ? parRacine.get(terme) : intersection(courant, parRacine.get(terme));
            etapes.add(new Etape(terme, courant == null ? tailleCorpus : courant.length, suivant.length, true));
            courant = suivant;
        }
        for (String terme : termes) {
            if (!retenus.contains(terme)) etapes.add(new Etape(terme, courant.length, 0, false));
        }

        // Classement : surtout la proximite, puis la qualite du prompt (note humaine, score NLP)
        List<Integer> tries = candidats;
        if (classerParNlp) {
            Map<Integer, Double> qualite = new HashMap<>();
            candidats.stream().limit(200).forEach(i -> qualite.put(i, qualite(prompts.get(i))));
            tries = candidats.stream().limit(200).sorted(Comparator.comparingDouble((Integer i) ->
                    POIDS_PERTINENCE * pertinence.get(i) / maximum + (1 - POIDS_PERTINENCE) * qualite.get(i)).reversed())
                    .toList();
        }
        List<Integer> retenusFinal = tries.stream().limit(nombre).toList();
        return new Resultat(etapes, retenusFinal.stream().map(prompts::get).toList(), candidats.size(),
                retenusFinal.stream().map(i -> pertinence.get(i) / maximum).toList(),
                retenusFinal.stream().map(i -> (double) motsTrouves.get(i) / richesse).toList(), richesse);
    }

    // Note humaine d'oasst2 quand elle existe, sinon score qualite de l'analyse NLP (0 a 1)
    static double qualite(Corpus.Prompt p) {
        double nlp = Lemmatizer.analyser(p.texte()).qualityDiagnostic().scoreGlobal() / 100.0;
        return p.qualite() != null ? 0.6 * p.qualite() + 0.4 * nlp : nlp;
    }

    static int[] intersection(int[] a, int[] b) {
        int[] resultat = new int[Math.min(a.length, b.length)];
        int i = 0, j = 0, n = 0;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                resultat[n++] = a[i];
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }
        return Arrays.copyOf(resultat, n);
    }

    //   java -cp "bin:src/lib/*" corpus.RechercheProgressive "je veux des croquettes pour mon chaton"
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage : corpus.RechercheProgressive \"prompt\" [nombre]");
            System.exit(1);
        }
        long debut = System.currentTimeMillis();
        RechercheProgressive recherche = new RechercheProgressive(Corpus.charger(Corpus.DOSSIER));
        System.out.printf("Index construit en %.1f s%n%n", (System.currentTimeMillis() - debut) / 1000.0);

        String prompt = args[0];
        String langue = Lemmatizer.analyser(prompt).language();
        debut = System.currentTimeMillis();
        Resultat resultat = recherche.rechercher(prompt, langue, args.length > 1 ? Integer.parseInt(args[1]) : 5);
        for (Etape etape : resultat.etapes()) {
            System.out.printf("  %-14s %7d -> %6d%s%n", etape.racine(), etape.avant(), etape.apres(),
                    etape.retenue() ? "" : "   (saute : aucun prompt)");
        }
        System.out.printf("%n%d prompts proches, les %d meilleurs (%d ms) :%n", resultat.candidats(),
                resultat.meilleurs().size(), System.currentTimeMillis() - debut);
        for (Corpus.Prompt p : resultat.meilleurs()) {
            String texte = p.texte().replaceAll("\\s+", " ");
            System.out.printf("%n- [%s%s] %s%n", p.source(), p.qualite() == null ? "" : String.format(Locale.ROOT, ", qualite %.2f", p.qualite()),
                    texte.length() <= 300 ? texte : texte.substring(0, 300) + "…");
        }
    }
}
