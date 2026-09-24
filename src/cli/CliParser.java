package cli;

import nlp.PromptProfile;
import nlp.TypeOfPrompt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import util.Log;

// Analyseur syntaxique d'arguments CLI et formateur d'aide/verbose
public class CliParser {

    public static final String VERSION = "1.0.0";
    public static final String AUTHOR = "Pierre Untersinger (@kaets0ner / gGaToRr)";
    public static final String TOOL_NAME = "Prompting tool 4 a better work from AI (AIM2k26)";

    // Description d'une option : ce qu'elle fait du builder, et si elle consomme une valeur.
    // La forme "--option=valeur" est derivee automatiquement, sans declaration supplementaire.
    private record Option(String nom, boolean attendValeur, BiConsumer<CliArgs.Builder, String> appliquer) {}

    // Table de dispatch : chaque alias accepte pointe vers son option.
    // Ajouter un flag = ajouter une ligne ici, et rien d'autre.
    private static final Map<String, Option> OPTIONS = construireTableOptions();

    private static Map<String, Option> construireTableOptions() {
        Map<String, Option> table = new LinkedHashMap<>();

        declarerDrapeau(table, "help", "-h", "--help", (b, v) -> b.help(true));
        declarerDrapeau(table, "version", "-v", "--version", (b, v) -> b.version(true));
        declarerDrapeau(table, "verbose", "-V", "--verbose", (b, v) -> b.verbose(true));
        declarerDrapeau(table, "raw", "-r", "--raw", (b, v) -> b.raw(true));
        declarerDrapeau(table, "dry-run", "-n", "--dry-run", (b, v) -> b.dryRun(true));
        declarerDrapeau(table, "clipboard", "-C", "--clipboard", (b, v) -> b.clipboard(true));
        declarerDrapeau(table, "exec", "-e", "--exec", (b, v) -> b.exec(true));

        declarerValeur(table, "model", "-m", "--model", CliArgs.Builder::model);
        declarerValeur(table, "instruction", "-i", "--instruction", CliArgs.Builder::instruction);
        declarerValeur(table, "agent", "-a", "--agent", CliArgs.Builder::agent);
        declarerValeur(table, "output", "-o", "--output", CliArgs.Builder::output);
        declarerValeur(table, "template", "-t", "--template", CliArgs.Builder::template);
        declarerValeur(table, "domain", "-d", "--domain", CliArgs.Builder::domain);
        declarerValeur(table, "language", "-l", "--language", CliArgs.Builder::language);

        // Gestion du cycle de vie des modeles locaux (Issue #45)
        declarerDrapeau(table, "models-list", "-ml", "--models-list", (b, v) -> b.modelsList(true));
        declarerDrapeau(table, "models-purge", "-mp", "--models-purge", (b, v) -> b.modelsPurge(true));
        declarerValeur(table, "models-delete", "-md", "--models-delete", CliArgs.Builder::modelsDelete);
        declarerValeur(table, "models-info", "-mi", "--models-info", CliArgs.Builder::modelsInfo);
        declarerValeur(table, "models-install", "-mt", "--models-install", CliArgs.Builder::modelsInstall);
        declarerDrapeau(table, "models-check", "-mc", "--models-check", (b, v) -> b.modelsCheck(true));
        declarerDrapeau(table, "runtime-install", "-ri", "--runtime-install", (b, v) -> b.runtimeInstall(true));
        declarerDrapeau(table, "corpus-install", "-ci", "--corpus-install", (b, v) -> b.corpusInstall(true));
        declarerDrapeau(table, "yes", "-y", "--yes", (b, v) -> b.yes(true));
        declarerDrapeau(table, "improve-json", "--improve-json", "--improve-json", (b, v) -> b.improveJson(true));

        declarerValeur(table, "code", "-c", "--code", (b, v) -> b.code(resoudreContenuCode(v)));

        // -f alimente deux champs : le chemin conserve, et l'instruction lue depuis le fichier
        declarerValeur(table, "file", "-f", "--file", (b, v) -> {
            b.filePath(v);
            b.instruction(resoudreContenuFichier(v));
        });

        return table;
    }

    private static void declarerDrapeau(Map<String, Option> table, String nom, String court, String longue,
                                        BiConsumer<CliArgs.Builder, String> appliquer) {
        Option option = new Option(nom, false, appliquer);
        table.put(court, option);
        table.put(longue, option);
    }

    private static void declarerValeur(Map<String, Option> table, String nom, String court, String longue,
                                       BiConsumer<CliArgs.Builder, String> appliquer) {
        Option option = new Option(nom, true, appliquer);
        table.put(court, option);
        table.put(longue, option);
    }

    // Parse le tableau d'arguments de la ligne de commande
    public static CliArgs parse(String[] args) {
        if (args == null || args.length == 0) {
            return CliArgs.builder().build();
        }

        CliArgs.Builder builder = CliArgs.builder();
        List<String> positionalArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.isEmpty()) continue;

            String cle = arg;
            String valeurAttachee = null;

            // Forme "--option=valeur" / "-o=valeur" : on ne coupe que sur un vrai flag,
            // pour qu'un argument positionnel contenant "=" reste intact.
            int positionEgal = arg.indexOf('=');
            if (arg.startsWith("-") && positionEgal > 0) {
                cle = arg.substring(0, positionEgal);
                valeurAttachee = arg.substring(positionEgal + 1);
            }

            Option option = OPTIONS.get(cle);

            if (option == null) {
                // Flag inconnu : ignore en silence. Tout le reste est un argument positionnel.
                if (!arg.startsWith("-")) {
                    positionalArgs.add(arg);
                }
                continue;
            }

            if (!option.attendValeur()) {
                option.appliquer().accept(builder, null);
                continue;
            }

            String valeur = valeurAttachee;
            if (valeur == null && i + 1 < args.length) {
                valeur = args[++i];
            }
            // Valeur absente (flag en fin de ligne) : l'option est simplement ignoree
            if (valeur != null) {
                option.appliquer().accept(builder, valeur.trim());
            }
        }

        // Si aucune instruction explicite (-i) n'a été passée mais qu'il y a des arguments positionnels
        CliArgs parsed = builder.build();
        if (!parsed.hasInstruction() && !positionalArgs.isEmpty()) {
            builder.instruction(String.join(" ", positionalArgs));
        }

        return builder.build();
    }

    // Résout le contenu : si la cible est un fichier existant, on lit son contenu, sinon on utilise la chaîne
    private static String resoudreContenuCode(String input) {
        if (input == null || input.isBlank()) return "";
        try {
            Path path = Path.of(input.trim());
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return Files.readString(path).trim();
            }
        } catch (Exception e) {
            // Pas un chemin de fichier valide, on traite comme un snippet inline
            Log.exceptionIgnoree("Resolution de -c/--code comme chemin, traite comme snippet inline", e);
        }
        return input.trim();
    }

    // Lit le contenu d'un fichier spécifié par -f/--file
    private static String resoudreContenuFichier(String chemin) {
        if (chemin == null || chemin.isBlank()) return "";
        try {
            Path path = Path.of(chemin.trim());
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return Files.readString(path).trim();
            } else {
                Log.avertir("Le fichier specifie n'existe pas : " + chemin);
            }
        } catch (IOException e) {
            Log.erreur("Lecture impossible du fichier " + chemin, e);
        }
        return "";
    }

    // Bannière d'aide stylisée identique au Menu d'accueil
    public static String getHelpBanner() {
        return """
                *------------------------------------------*
                *  Prompting tool 4 a better work from AI  *
                *------------------------------------------*
                *        MANUEL D'UTILISATION (CLI)        *
                *------------------------------------------*

                Usage:
                  java -cp "bin:lib/*" Main [OPTIONS] [INSTRUCTION]

                Arguments principaux :
                  -h, --help                     Afficher ce manuel d'utilisation et quitter.
                  -v, --version                  Afficher la version et les métadonnées de release.
                  -i, --instruction <texte>      Passer le prompt/instruction directement en argument.
                  -c, --code <snippet|fichier>   Injecter un extrait de code ou le contenu d'un fichier source.
                  -V, --verbose                  Activer le mode verbeux détaillant l'analyse NLP et les métriques.

                Inférence Locale & LLMs Embarqués (100% Hors-ligne) :
                  -e, --exec                     Exécuter directement le prompt optimisé avec le LLM local expert.
                  -m, --model <nom>              Forcer un modèle local (auto, qwen, gemma, deepseek, smollm).

                Gestion des modèles locaux :
                  -ml, --models-list             Lister les modèles, leur statut, taille et date d'installation.
                  -mi, --models-info <nom>       Fiche technique détaillée d'un modèle.
                  -mt, --models-install <nom>    Télécharger et installer un modèle (ex: qwen-coder).
                  -mc, --models-check            Vérifier les modèles installés (intégrité + test de génération).
                  -ri, --runtime-install         Installer le moteur llama.cpp qui exécute les modèles.
                  -ci, --corpus-install          Installer la base de prompts (recherche du prompt de référence).
                  -md, --models-delete <nom>     Supprimer un modèle installé, après confirmation.
                  -mp, --models-purge            Supprimer tous les modèles et réinitialiser l'autorisation.
                  -y,  --yes                     Répondre oui aux confirmations (usage par l'extension).
                  --improve-json                 Améliorer le prompt JSON lu sur l'entrée standard (usage par l'extension).

                Options avancées & personnalisation :
                  -a, --agent <nom>              Adapter le format pour un LLM (claude, gpt, deepseek, gemini, llama).
                  -t, --template <nom>           Forcer un template spécifique (ex: architecture_systeme, feynman).
                  -d, --domain <nom>             Forcer un domaine métier (ex: software_engineering, cybersecurity).
                  -l, --language <code_langue>   Définir la langue cible du prompt généré (fr, en, es, de).
                  -o, --output <format|chemin>   Format de sortie (txt, md, json) ou chemin de fichier d'export.
                  -r, --raw                      Mode sortie brute sans bannières ni séparateurs (pipe Unix).
                  -f, --file <chemin>            Lire l'instruction source depuis un fichier texte/MD.
                  -n, --dry-run                  Analyse NLP seule (profil sémantique, BPE) sans génération.
                  -C, --clipboard                Copier directement le résultat dans le presse-papiers.

                Exemples :
                  java -cp "bin:lib/*" Main -i "Explique le tri fusion en Java" -e
                  java -cp "bin:lib/*" Main -i "Trouve le bug" -c "src/Utils.java" -e -m qwen
                  java -cp "bin:lib/*" Main -i "Compare Postgres et MongoDB" -e -m deepseek
                  java -cp "bin:lib/*" Main -h
                """;
    }

    // Informations de version du projet
    public static String getVersionInfo() {
        return TOOL_NAME + "\n"
                + "Version : " + VERSION + "\n"
                + "Auteur  : " + AUTHOR + "\n"
                + "Java    : " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n"
                + "OS      : " + System.getProperty("os.name") + " " + System.getProperty("os.arch");
    }

    // Rapport d'analyse verbeux exhaustif (NLP, tokens, archétypes, coûts)
    public static String formatVerboseReport(PromptProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==========================================================\n");
        sb.append("           RAPPORT DÉTAILLÉ D'ANALYSE NLP (-V)            \n");
        sb.append("==========================================================\n");

        // 1. Nettoyage
        sb.append("\n[1. SANITIZATION & ÉLISIONS]\n");
        sb.append("  • Texte brut     : \"").append(profile.rawText()).append("\"\n");
        sb.append("  • Texte nettoyé  : \"").append(profile.sanitizedText()).append("\"\n");

        // 2. Lemmes & Code
        sb.append("\n[2. LEMMATISATION & CARACTÉRISTIQUES]\n");
        sb.append("  • Tokens extraits  : ").append(profile.tokens()).append("\n");
        sb.append("  • Lemmes fréquents : ").append(profile.lemmaFrequencies().keySet()).append("\n");
        sb.append("  • Densité de code  : ").append(String.format("%.1f%%", profile.codeDensity() * 100)).append("\n");
        sb.append("  • Est une commande : ").append(profile.isCommand()).append("\n");
        sb.append("  • Est une question : ").append(profile.isQuestion()).append("\n");
        sb.append("  • Langue détectée  : ").append(profile.language()).append("\n");

        // 3. Technologies
        if (!profile.detectedTechnologies().isEmpty()) {
            sb.append("\n[3. TECHNOLOGIES DÉTECTÉES]\n");
            sb.append("  • Stack technique  : ").append(String.join(", ", profile.detectedTechnologies())).append("\n");
        }

        // 4. Domaine & Persona
        if (profile.domainInfo() != null) {
            sb.append("\n[4. DOMAINE MÉTIER & PERSONA EXPERT]\n");
            sb.append("  • Famille domaine  : ").append(profile.domainInfo().domainName()).append(profile.domainInfo().isDomainIdentified() ? "" : " (Non spécifique)").append("\n");
            sb.append("  • Sujet pivot      : ").append(profile.domainInfo().extractedTopic()).append("\n");
            sb.append("  • Persona assigné  : ").append(profile.domainInfo().expertPersona()).append("\n");
        }
        if (profile.themes() != null) {
            sb.append("  • Thèmes           : ").append(String.join(", ",
                    profile.themes().stream().map(nlp.ThemeClassifier::libelle).toList())).append("\n");
        }

        // 5. Décomposition
        if (profile.decomposition() != null && profile.decomposition().hasMultipleObjectives()) {
            sb.append("\n[5. DÉCOMPOSITION DU FLUX DE PENSÉE]\n");
            for (int i = 0; i < profile.decomposition().objectives().size(); i++) {
                sb.append("  • Objectif ").append(i + 1).append(" : ").append(profile.decomposition().objectives().get(i)).append("\n");
            }
        }

        // 6. Distribution Softmax des 7 Archétypes
        sb.append("\n[6. DISTRIBUTION PROBABILISTE SOFTMAX]\n");
        TypeOfPrompt winner = profile.classification().primaryType();
        Map<TypeOfPrompt, Double> dist = profile.classification().distribution();
        for (TypeOfPrompt type : TypeOfPrompt.values()) {
            double prob = dist != null ? dist.getOrDefault(type, 0.0) : 0.0;
            String marker = (type == winner) ? " -> [GAGNANT]" : "";
            sb.append(String.format("  • %-26s : %5.1f%% %s\n", type.getLabel(), prob, marker));
        }
        sb.append("  Niveau de confiance : ").append(profile.classification().confidenceLevel()).append("\n");

        // 7. Tokens & Coûts API
        double baseCost = profile.tokenMetrics().estimatedCostDollars();
        sb.append("\n[7. MÉTRIQUES DE TOKENS & ESTIMATION DES COÛTS]\n");
        sb.append("  • Caractètres       : ").append(profile.tokenMetrics().characterCount()).append("\n");
        sb.append("  • Mots             : ").append(profile.tokenMetrics().wordCount()).append("\n");
        sb.append("  • Tokens BPE est.  : ").append(profile.tokenMetrics().estimatedTokens()).append("\n");
        sb.append("  • Context Fitness  : ").append(profile.tokenMetrics().contextWindowFitness()).append("\n");
        sb.append("  • Coût OpenAI/LLM  : $").append(String.format("%.6f", baseCost)).append("\n");

        // 8. Qualité globale
        sb.append("\n[8. DIAGNOSTIC DE QUALITÉ DU PROMPT]\n");
        sb.append("  • Score Global     : ").append(profile.qualityDiagnostic().scoreGlobal()).append("/100\n");
        sb.append("  • Clarté           : ").append(profile.qualityDiagnostic().scoreClarte()).append("/40\n");
        sb.append("  • Contexte         : ").append(profile.qualityDiagnostic().scoreContexte()).append("/30\n");
        sb.append("  • Contraintes      : ").append(profile.qualityDiagnostic().scoreContraintes()).append("/30\n");
        sb.append("==========================================================\n");

        return sb.toString();
    }
}
