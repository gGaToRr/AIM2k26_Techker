package nlp;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Moteur principal pour l'analyse NLP, le nettoyage et la lemmatisation
public class Lemmatizer {

    private static final Pattern MOTS_PATTERN = Pattern.compile("[^\\p{L}\\p{N}+#]+");
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
    private static final Pattern DIACRITIQUES_PATTERN = Pattern.compile("\\p{M}");

    private static final Pattern CODE_KEYWORDS_PATTERN = Pattern.compile(
            "\\b(public|private|protected|class|interface|record|enum|def|fn|function|const|let|var|import|package|return|async|await|try|catch|throws|void|int|String|boolean|float|double|val|namespace|struct|template|include)\\b"
    );
    private static final Pattern CODE_SYNTAX_PATTERN = Pattern.compile("[{};\\[\\]()=>#$]");

    // Verbes d'action quand l'utilisateur donne un ordre
    private static final Set<String> VERBES_ACTION_COMMANDE = Set.of(
            "cree", "creer", "genere", "generer", "ecris", "ecrire", "redige", "rediger",
            "fais", "faire", "donne", "donner", "liste", "lister", "trouve", "trouver",
            "traduis", "traduire", "corrige", "corriger", "ameliore", "ameliorer", "optimise", "optimiser",
            "refactore", "refactorer", "debug", "debogue", "deboguer", "explique", "expliquer",
            "concois", "concevoir", "architecture", "structure", "structurez",
            "create", "generate", "write", "make", "give", "list", "find", "translate", "fix", "improve", "explain"
    );

    // Mots interrogatifs quand l'utilisateur pose une question
    private static final Set<String> MOTS_INTERROGATIFS = Set.of(
            "pourquoi", "comment", "quand", "qui", "quoi", "quel", "quelle", "quels", "quelles",
            "ou", "combien", "estce", "estceque", "why", "how", "when", "who", "what", "which", "where"
    );

    // Expressions de plusieurs mots clés
    private static final Map<String, String> EXPRESSIONS_COMPOSEES = Map.ofEntries(
            Map.entry("en anglais", "traduir"),
            Map.entry("en francais", "traduir"),
            Map.entry("en espagnol", "traduir"),
            Map.entry("revue de code", "code"),
            Map.entry("code review", "code"),
            Map.entry("arborescence des fichiers", "code"),
            Map.entry("arborescence du projet", "code"),
            Map.entry("architecture logicielle", "code"),
            Map.entry("structure de dossier", "code"),
            Map.entry("jeu video", "code"),
            Map.entry("qu est ce que", "question"),
            Map.entry("c est quoi", "question"),
            Map.entry("pas a pas", "expliqu"),
            Map.entry("step by step", "expliqu"),
            Map.entry("ligne par ligne", "expliqu")
    );

    // Mots vides sans valeur sémantique
    private static final Set<String> STOP_WORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "du", "de", "d", "l", "et", "ou",
            "a", "au", "aux", "en", "dans", "pour", "par", "sur", "avec", "sans",
            "ce", "cet", "cette", "ces", "mon", "ton", "son", "notre", "votre", "leur",
            "je", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles", "me", "te", "se",
            "est", "sont", "ete", "etre", "avoir", "ai", "as", "avons", "avez", "ont",
            "salut", "bonjour", "besoin", "besoins", "tout",
            "the", "an", "and", "or", "in", "at", "for", "to", "with", "is", "are"
    );

    // Dictionnaire des racines métier
    private static final Map<String, String> DICTIONNAIRE_LEMMES = Map.ofEntries(
            // Code, Dev & Architecture
            Map.entry("programme", "code"),
            Map.entry("fonction", "code"),
            Map.entry("methode", "code"),
            Map.entry("script", "code"),
            Map.entry("algorithme", "code"),
            Map.entry("developpe", "code"),
            Map.entry("programmer", "code"),
            Map.entry("coder", "code"),
            Map.entry("architecture", "code"),
            Map.entry("arborescence", "code"),
            Map.entry("dossier", "code"),
            Map.entry("fichier", "code"),
            Map.entry("fichiers", "code"),
            Map.entry("structure", "code"),
            Map.entry("jeu", "code"),
            Map.entry("game", "code"),
            Map.entry("unity", "code"),
            Map.entry("unreal", "code"),
            Map.entry("cpp", "code"),
            Map.entry("projet", "code"),
            Map.entry("endpoint", "code"),
            Map.entry("api", "code"),

            // Debug & Fix
            Map.entry("debug", "debug"),
            Map.entry("deboguer", "debug"),
            Map.entry("erreur", "debug"),
            Map.entry("exception", "debug"),
            Map.entry("bug", "debug"),
            Map.entry("crash", "debug"),
            Map.entry("refactor", "code"),
            Map.entry("refactoriser", "code"),

            // Traduction
            Map.entry("traduire", "traduir"),
            Map.entry("traduis", "traduir"),
            Map.entry("traduction", "traduir"),
            Map.entry("translate", "traduir"),
            Map.entry("anglais", "traduir"),
            Map.entry("francais", "traduir"),
            Map.entry("espagnol", "traduir"),

            // Correction & Amélioration
            Map.entry("corrige", "corrig"),
            Map.entry("corriger", "corrig"),
            Map.entry("correction", "corrig"),
            Map.entry("orthographe", "corrig"),
            Map.entry("grammaire", "corrig"),
            Map.entry("reformule", "corrig"),
            Map.entry("reformuler", "corrig"),
            Map.entry("relecture", "corrig"),
            Map.entry("faute", "corrig"),

            // Création & Imagination
            Map.entry("invente", "creer"),
            Map.entry("inventer", "creer"),
            Map.entry("imagine", "creer"),
            Map.entry("imaginer", "creer"),
            Map.entry("redige", "creer"),
            Map.entry("rediger", "creer"),
            Map.entry("histoire", "creer"),
            Map.entry("poeme", "creer"),
            Map.entry("scenario", "creer"),

            // Questions & Explications
            Map.entry("explique", "expliqu"),
            Map.entry("expliquer", "expliqu"),
            Map.entry("explication", "expliqu"),
            Map.entry("definition", "expliqu"),
            Map.entry("pourquoi", "question"),
            Map.entry("quand", "question")
    );

    // Poids pour donner plus d'importance aux verbes d'action
    private static final Map<String, Double> POIDS_LEMMES = Map.of(
            "traduir", 3.0,
            "corrig", 2.8,
            "debug", 2.8,
            "code", 2.5,
            "creer", 2.2,
            "expliqu", 2.0,
            "question", 1.8
    );

    // Méthode principale qui exécute toute la chaîne d'analyse
    public static PromptProfile analyser(String rawPrompt) {
        if (rawPrompt == null || rawPrompt.isBlank()) {
            return new PromptProfile("", "", List.of(), Map.of(), Map.of(), 0.0, false, false, "UNKNOWN", List.of(), Optional.empty(), TokenCounter.analyser(""), new PromptQualityScorer.Diagnostic(0,0,0,0,List.of(),List.of()), new PromptClassifier.ClassificationResult(TypeOfPrompt.FACTUALQUESTIONS, 0, PromptClassifier.ConfidenceLevel.LOW, Map.of(), "Vide"));
        }

        // Nettoyage et découpage
        String sanitized = Sanitzer.nettoyerPrompt(rawPrompt);
        List<String> tokens = tokeniser(sanitized);
        Map<String, Integer> frequences = compterFrequences(rawPrompt);
        Map<String, Double> weightedScores = calculerPoidsSemantiques(frequences);
        double codeDensity = calculerDensiteCode(rawPrompt);
        boolean isQuestion = detecterQuestion(rawPrompt, tokens);
        boolean isCommand = detecterCommande(tokens);
        String language = detecterLangue(rawPrompt, tokens);

        // Détection technique et métriques
        List<String> techStack = TechStackDetector.detecterTechnologies(rawPrompt);
        Optional<String> langueCible = TechStackDetector.detecterLangueCibleTraduction(rawPrompt);
        TokenCounter.TokenMetrics tokenMetrics = TokenCounter.analyser(rawPrompt);
        PromptQualityScorer.Diagnostic qualityDiag = PromptQualityScorer.evaluer(rawPrompt, codeDensity >= 0.15 || !techStack.isEmpty(), isCommand, isQuestion);
        PromptClassifier.ClassificationResult classification = PromptClassifier.classifier(
                new PromptProfile(rawPrompt, sanitized, tokens, frequences, weightedScores, codeDensity, isQuestion, isCommand, language, techStack, langueCible, tokenMetrics, qualityDiag, null),
                techStack,
                langueCible
        );

        return new PromptProfile(
                rawPrompt,
                sanitized,
                tokens,
                frequences,
                weightedScores,
                codeDensity,
                isQuestion,
                isCommand,
                language,
                techStack,
                langueCible,
                tokenMetrics,
                qualityDiag,
                classification
        );
    }

    // Calcule la proportion de code dans le prompt
    public static double calculerDensiteCode(String text) {
        if (text == null || text.isBlank()) return 0.0;
        int codeHits = 0;

        Matcher keywordMatcher = CODE_KEYWORDS_PATTERN.matcher(text);
        while (keywordMatcher.find()) codeHits += 3;

        Matcher syntaxMatcher = CODE_SYNTAX_PATTERN.matcher(text);
        while (syntaxMatcher.find()) codeHits += 1;

        String[] words = text.split("\\s+");
        if (words.length == 0) return 0.0;

        double ratio = (double) codeHits / (words.length * 1.5);
        return Math.min(1.0, Math.max(0.0, ratio));
    }

    // Détecte si le prompt est une question
    public static boolean detecterQuestion(String rawText, List<String> tokens) {
        if (rawText.contains("?")) return true;
        for (String token : tokens) {
            if (MOTS_INTERROGATIFS.contains(token)) return true;
        }
        return false;
    }

    // Détecte si le prompt commence par une commande/verbe d'action
    public static boolean detecterCommande(List<String> tokens) {
        if (tokens.isEmpty()) return false;
        int limit = Math.min(5, tokens.size());
        for (int i = 0; i < limit; i++) {
            if (VERBES_ACTION_COMMANDE.contains(tokens.get(i))) return true;
        }
        return false;
    }

    // Multiplie les occurrences par les poids d'importance
    public static Map<String, Double> calculerPoidsSemantiques(Map<String, Integer> frequences) {
        Map<String, Double> scores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : frequences.entrySet()) {
            scores.put(entry.getKey(), entry.getValue() * POIDS_LEMMES.getOrDefault(entry.getKey(), 1.0));
        }
        return scores;
    }

    // Détecte si la langue principale est FR ou EN
    public static String detecterLangue(String rawText, List<String> tokens) {
        if (rawText.matches(".*[éèêëàâîïôûùç].*")) return "FR";
        int scoreFR = 0, scoreEN = 0;
        Set<String> markersFR = Set.of("le", "la", "les", "un", "une", "des", "dans", "avec", "pour", "est", "qui", "que", "je", "tu");
        Set<String> markersEN = Set.of("the", "and", "with", "for", "from", "this", "that", "how", "what", "why", "code", "file");

        for (String token : tokens) {
            if (markersFR.contains(token)) scoreFR++;
            if (markersEN.contains(token)) scoreEN++;
        }
        return scoreEN > scoreFR ? "EN" : "FR";
    }

    // Supprime les accents
    public static String retirerAccents(String texte) {
        if (texte == null) return "";
        return DIACRITIQUES_PATTERN.matcher(Normalizer.normalize(texte, Normalizer.Form.NFD)).replaceAll("");
    }

    // Sépare le CamelCase et snake_case
    public static String normaliserIdentifiants(String texte) {
        if (texte == null) return "";
        return CAMEL_CASE_PATTERN.matcher(texte.replace('_', ' ')).replaceAll(" ");
    }

    // Découpe le texte en mots
    public static List<String> tokeniser(String texte) {
        if (texte == null || texte.isBlank()) return Collections.emptyList();
        List<String> tokens = new ArrayList<>();
        for (String mot : MOTS_PATTERN.split(texte.toLowerCase())) {
            String propre = mot.trim();
            if (propre.length() > 1 && !STOP_WORDS.contains(propre)) {
                tokens.add(propre);
            }
        }
        return tokens;
    }

    // Réduit un mot à sa racine
    public static String lemmatiserMot(String mot) {
        if (mot == null || mot.isBlank()) return "";
        String cle = mot.toLowerCase();
        if (DICTIONNAIRE_LEMMES.containsKey(cle)) return DICTIONNAIRE_LEMMES.get(cle);
        return raciniserSuffixe(cle);
    }

    // Compte la fréquence des racines trouvées
    public static Map<String, Integer> compterFrequences(String rawText) {
        String normalise = normaliserIdentifiants(retirerAccents(rawText.toLowerCase()));
        List<String> lemmes = new ArrayList<>();

        for (Map.Entry<String, String> entry : EXPRESSIONS_COMPOSEES.entrySet()) {
            if (normalise.contains(entry.getKey())) lemmes.add(entry.getValue());
        }

        for (String mot : MOTS_PATTERN.split(normalise)) {
            String propre = mot.trim();
            if (propre.length() > 1 && !STOP_WORDS.contains(propre)) {
                String lemme = lemmatiserMot(propre);
                if (!lemme.isBlank()) {
                    lemmes.add(lemme);
                }
            }
        }

        Map<String, Integer> frequences = new HashMap<>();
        for (String lemme : lemmes) frequences.put(lemme, frequences.getOrDefault(lemme, 0) + 1);
        return frequences;
    }

    // Racinisation des terminaisons
    private static String raciniserSuffixe(String mot) {
        if (mot.length() <= 3) return mot;
        if (mot.endsWith("issantes") || mot.endsWith("issants") || mot.endsWith("issant")) return mot.substring(0, mot.length() - 6);
        if (mot.endsWith("ements") || mot.endsWith("ement") || mot.endsWith("ations") || mot.endsWith("ation")) return mot.substring(0, mot.length() - 5);
        if (mot.endsWith("er") || mot.endsWith("ez") || mot.endsWith("es") || mot.endsWith("ed")) return mot.substring(0, mot.length() - 2);
        if (mot.endsWith("s") || mot.endsWith("x")) return mot.substring(0, mot.length() - 1);
        return mot;
    }
}
