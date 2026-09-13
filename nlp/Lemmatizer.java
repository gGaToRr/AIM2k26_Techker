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
            "\\b(public|private|protected|class|interface|record|enum|def|fn|function|const|let|var|import|package|return|async|await|try|catch|throws|void|int|String|boolean|float|double|val|namespace|struct|template|include|css|html|flex|grid|margin|padding|border|radius|hover|shadow)\\b"
    );
    private static final Pattern CODE_SYNTAX_PATTERN = Pattern.compile("[{};\\[\\]()=>#$]");

    // Verbes d'action quand l'utilisateur donne un ordre
    private static final Set<String> VERBES_ACTION_COMMANDE = Set.of(
            "cree", "creer", "genere", "generer", "ecris", "ecrire", "redige", "rediger",
            "fais", "faire", "donne", "donner", "liste", "lister", "trouve", "trouver",
            "traduis", "traduire", "corrige", "corriger", "ameliore", "ameliorer", "optimise", "optimiser",
            "refactore", "refactorer", "debug", "debogue", "deboguer", "explique", "expliquer",
            "concois", "concevoir", "architecture", "structure", "structurez", "refaire", "reforme",
            "apprendre", "enseigne", "enseigner",
            "create", "generate", "write", "make", "give", "list", "find", "translate", "fix", "improve", "explain", "learn"
    );

    // Mots interrogatifs quand l'utilisateur pose une question
    private static final Set<String> MOTS_INTERROGATIFS = Set.of(
            "pourquoi", "comment", "quand", "qui", "quoi", "quel", "quelle", "quels", "quelles",
            "ou", "combien", "estce", "estceque", "why", "how", "when", "who", "what", "which", "where"
    );

    // Expressions de plusieurs mots clés
    private static final Map<String, String> EXPRESSIONS_COMPOSEES = Map.ofEntries(
            // Comparaison & Décision
            Map.entry("quelle est la difference", "compar"),
            Map.entry("quelle difference", "compar"),
            Map.entry("avantages et inconvenients", "compar"),
            Map.entry("lequel choisir", "compar"),
            Map.entry("comparatif entre", "compar"),
            Map.entry("pour ou contre", "compar"),
            Map.entry("benchmark de", "compar"),

            // Protocole & Recette
            Map.entry("recette de", "protocol"),
            Map.entry("recette pour", "protocol"),
            Map.entry("protocole de", "protocol"),
            Map.entry("procedure de", "protocol"),
            Map.entry("etapes pour", "protocol"),
            Map.entry("mode d emploi", "protocol"),
            Map.entry("guide de fabrication", "protocol"),

            // Apprentissage & Tutoriel
            Map.entry("je veux apprendre", "apprend"),
            Map.entry("guide du debutant", "apprend"),
            Map.entry("premiers pas", "apprend"),
            Map.entry("pas a pas", "apprend"),
            Map.entry("step by step", "apprend"),
            Map.entry("ligne par ligne", "apprend"),
            Map.entry("comment on fait", "apprend"),
            Map.entry("comment faire", "apprend"),

            // Conception & Architecture
            Map.entry("architecture logicielle", "architect"),
            Map.entry("arborescence des fichiers", "architect"),
            Map.entry("arborescence du projet", "architect"),
            Map.entry("structure de dossier", "architect"),
            Map.entry("structure des dossiers", "architect"),
            Map.entry("design du site", "architect"),
            Map.entry("refaire le design", "architect"),
            Map.entry("design system", "architect"),

            // Dépannage & Diagnostic
            Map.entry("revue de code", "depann"),
            Map.entry("code review", "depann"),
            Map.entry("audit de securite", "depann"),
            Map.entry("cause racine", "depann"),
            Map.entry("root cause", "depann"),

            // Traduction & Rédaction
            Map.entry("en anglais", "traduir"),
            Map.entry("en francais", "traduir"),
            Map.entry("en espagnol", "traduir"),

            // Concept & Vulgarisation
            Map.entry("qu est ce que", "concept"),
            Map.entry("c est quoi", "concept"),
            Map.entry("comment ca marche", "concept"),
            Map.entry("explication simple", "concept"),
            Map.entry("theorie de", "concept"),
            Map.entry("histoire de", "concept")
    );

    // Mots vides sans valeur sémantique
    private static final Set<String> STOP_WORDS = Set.of(
            "le", "la", "les", "un", "une", "des", "du", "de", "d", "l", "et", "ou",
            "a", "au", "aux", "en", "dans", "pour", "par", "sur", "avec", "sans",
            "ce", "cet", "cette", "ces", "mon", "ton", "son", "notre", "votre", "leur",
            "je", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles", "me", "te", "se",
            "est", "sont", "ete", "etre", "avoir", "ai", "as", "avons", "avez", "ont",
            "salut", "bonjour", "besoin", "besoins", "tout", "alors", "voila", "parce", "deja",
            "the", "an", "and", "or", "in", "at", "for", "to", "with", "is", "are"
    );

    // Dictionnaire des racines métier
    private static final Map<String, String> DICTIONNAIRE_LEMMES = Map.ofEntries(
            // 1. Apprentissage & Tutoriel
            Map.entry("apprendre", "apprend"),
            Map.entry("apprentissage", "apprend"),
            Map.entry("tutoriel", "apprend"),
            Map.entry("tuto", "apprend"),
            Map.entry("guide", "apprend"),
            Map.entry("debutant", "apprend"),
            Map.entry("initiation", "apprend"),
            Map.entry("initier", "apprend"),
            Map.entry("cours", "apprend"),
            Map.entry("exercice", "apprend"),
            Map.entry("exercices", "apprend"),
            Map.entry("entrainement", "apprend"),
            Map.entry("pedagogie", "apprend"),

            // 2. Conception & Architecture
            Map.entry("architecture", "architect"),
            Map.entry("arborescence", "architect"),
            Map.entry("concevoir", "architect"),
            Map.entry("conception", "architect"),
            Map.entry("structure", "architect"),
            Map.entry("structurer", "architect"),
            Map.entry("plan", "architect"),
            Map.entry("schema", "architect"),
            Map.entry("diagramme", "architect"),
            Map.entry("programme", "code"),
            Map.entry("fonction", "code"),
            Map.entry("methode", "code"),
            Map.entry("script", "code"),
            Map.entry("algorithme", "code"),
            Map.entry("developpe", "code"),
            Map.entry("programmer", "code"),
            Map.entry("coder", "code"),
            Map.entry("css", "architect"),
            Map.entry("html", "architect"),
            Map.entry("tailwind", "architect"),
            Map.entry("responsive", "architect"),
            Map.entry("frontend", "architect"),
            Map.entry("ui", "architect"),
            Map.entry("ux", "architect"),
            Map.entry("unity", "code"),
            Map.entry("unreal", "code"),
            Map.entry("api", "code"),
            Map.entry("endpoint", "code"),

            // 3. Dépannage & Diagnostic
            Map.entry("debug", "depann"),
            Map.entry("debogue", "depann"),
            Map.entry("deboguer", "depann"),
            Map.entry("erreur", "depann"),
            Map.entry("exception", "depann"),
            Map.entry("bug", "depann"),
            Map.entry("crash", "depann"),
            Map.entry("panne", "depann"),
            Map.entry("probleme", "depann"),
            Map.entry("reparer", "depann"),
            Map.entry("fixer", "depann"),
            Map.entry("fix", "depann"),
            Map.entry("audit", "depann"),
            Map.entry("review", "depann"),
            Map.entry("refactor", "depann"),
            Map.entry("refactoriser", "depann"),

            // 4. Création & Rédaction
            Map.entry("invente", "creer"),
            Map.entry("inventer", "creer"),
            Map.entry("imagine", "creer"),
            Map.entry("imaginer", "creer"),
            Map.entry("redige", "creer"),
            Map.entry("rediger", "creer"),
            Map.entry("histoire", "creer"),
            Map.entry("poeme", "creer"),
            Map.entry("scenario", "creer"),
            Map.entry("recit", "creer"),
            Map.entry("roman", "creer"),
            Map.entry("dialogue", "creer"),
            Map.entry("brainstorming", "creer"),
            Map.entry("idee", "creer"),
            Map.entry("idees", "creer"),
            Map.entry("corrige", "corrig"),
            Map.entry("corriger", "corrig"),
            Map.entry("correction", "corrig"),
            Map.entry("orthographe", "corrig"),
            Map.entry("grammaire", "corrig"),
            Map.entry("reformule", "corrig"),
            Map.entry("reformuler", "corrig"),
            Map.entry("relecture", "corrig"),
            Map.entry("traduire", "traduir"),
            Map.entry("traduis", "traduir"),
            Map.entry("traduction", "traduir"),
            Map.entry("translate", "traduir"),

            // 5. Protocole & Recette
            Map.entry("recette", "protocol"),
            Map.entry("protocole", "protocol"),
            Map.entry("procedure", "protocol"),
            Map.entry("etape", "protocol"),
            Map.entry("etapes", "protocol"),
            Map.entry("cuisiner", "protocol"),
            Map.entry("cuisine", "protocol"),
            Map.entry("cuisson", "protocol"),
            Map.entry("ingredient", "protocol"),
            Map.entry("ingredients", "protocol"),
            Map.entry("dosage", "protocol"),
            Map.entry("preparation", "protocol"),
            Map.entry("preparer", "protocol"),
            Map.entry("fabriquer", "protocol"),
            Map.entry("assemblage", "protocol"),
            Map.entry("checklist", "protocol"),
            Map.entry("deployer", "protocol"),

            // 6. Comparaison & Décision
            Map.entry("comparer", "compar"),
            Map.entry("comparaison", "compar"),
            Map.entry("comparatif", "compar"),
            Map.entry("difference", "compar"),
            Map.entry("versus", "compar"),
            Map.entry("vs", "compar"),
            Map.entry("choisir", "compar"),
            Map.entry("choix", "compar"),
            Map.entry("meilleur", "compar"),
            Map.entry("avantage", "compar"),
            Map.entry("avantages", "compar"),
            Map.entry("inconvenient", "compar"),
            Map.entry("inconvenients", "compar"),
            Map.entry("arbitrage", "compar"),
            Map.entry("benchmark", "compar"),

            // 7. Concept & Vulgarisation
            Map.entry("definition", "concept"),
            Map.entry("concept", "concept"),
            Map.entry("theorie", "concept"),
            Map.entry("explique", "expliqu"),
            Map.entry("expliquer", "expliqu"),
            Map.entry("explication", "expliqu"),
            Map.entry("comprendre", "expliqu"),
            Map.entry("pourquoi", "question"),
            Map.entry("feynman", "concept"),
            Map.entry("vulgarisation", "concept"),
            Map.entry("encyclopedie", "concept")
    );

    // Poids sémantiques pour orienter la classification
    private static final Map<String, Double> POIDS_LEMMES = Map.ofEntries(
            Map.entry("compar", 3.2),
            Map.entry("protocol", 3.0),
            Map.entry("depann", 2.8),
            Map.entry("apprend", 2.8),
            Map.entry("architect", 2.6),
            Map.entry("code", 2.5),
            Map.entry("creer", 2.4),
            Map.entry("corrig", 2.4),
            Map.entry("traduir", 2.6),
            Map.entry("concept", 2.5),
            Map.entry("expliqu", 2.5),
            Map.entry("question", 2.0)
    );

    // Méthode principale qui exécute toute la chaîne d'analyse
    public static PromptProfile analyser(String rawPrompt) {
        if (rawPrompt == null || rawPrompt.isBlank()) {
            return new PromptProfile("", "", List.of(), Map.of(), Map.of(), 0.0, false, false, "UNKNOWN", List.of(), Optional.empty(), new DomainExtractor.DomainInfo("Général", "", "", false), new QuestionDecomposer.DecompositionResult(false, List.of(), ""), TokenCounter.analyser(""), new PromptQualityScorer.Diagnostic(0,0,0,0,List.of(),List.of()), new PromptClassifier.ClassificationResult(TypeOfPrompt.CONCEPT_VULGARISATION, 0, PromptClassifier.ConfidenceLevel.LOW, Map.of(), "Vide"));
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

        // Détection technique, domaine sémantique, décomposition et métriques
        List<String> techStack = TechStackDetector.detecterTechnologies(rawPrompt);
        Optional<String> langueCible = TechStackDetector.detecterLangueCibleTraduction(rawPrompt);
        DomainExtractor.DomainInfo domainInfo = DomainExtractor.analyser(rawPrompt);
        QuestionDecomposer.DecompositionResult decomposition = QuestionDecomposer.decomposer(rawPrompt);
        TokenCounter.TokenMetrics tokenMetrics = TokenCounter.analyser(rawPrompt);
        PromptQualityScorer.Diagnostic qualityDiag = PromptQualityScorer.evaluer(rawPrompt, codeDensity >= 0.15 || !techStack.isEmpty(), isCommand, isQuestion);
        PromptClassifier.ClassificationResult classification = PromptClassifier.classifier(
                new PromptProfile(rawPrompt, sanitized, tokens, frequences, weightedScores, codeDensity, isQuestion, isCommand, language, techStack, langueCible, domainInfo, decomposition, tokenMetrics, qualityDiag, null),
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
                domainInfo,
                decomposition,
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
