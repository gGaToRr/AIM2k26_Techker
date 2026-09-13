package cli;

import nlp.PromptProfile;
import nlp.TypeOfPrompt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Analyseur syntaxique d'arguments CLI et formateur d'aide/verbose
public class CliParser {

    public static final String VERSION = "1.1.0";
    public static final String AUTHOR = "Pierre Untersinger (@kaets0ner / gGaToRr)";
    public static final String TOOL_NAME = "Prompting tool 4 a better work from AI (AIM2k26)";

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

            // Flags d'aide
            if (arg.equals("-h") || arg.equals("--help")) {
                builder.help(true);
            }
            // Flags de version
            else if (arg.equals("-v") || arg.equals("--version")) {
                builder.version(true);
            }
            // Mode verbeux
            else if (arg.equals("-V") || arg.equals("--verbose")) {
                builder.verbose(true);
            }
            // Sortie brute (raw)
            else if (arg.equals("-r") || arg.equals("--raw")) {
                builder.raw(true);
            }
            // Mode analyse seule (dry-run)
            else if (arg.equals("-n") || arg.equals("--dry-run")) {
                builder.dryRun(true);
            }
            // Presse-papiers
            else if (arg.equals("-C") || arg.equals("--clipboard")) {
                builder.clipboard(true);
            }
            // Exécution locale par LLM embarqué (-e ou --exec)
            else if (arg.equals("-e") || arg.equals("--exec")) {
                builder.exec(true);
            }
            // Spécification de modèle local (-m ou --model)
            else if (arg.equals("-m") || arg.equals("--model")) {
                if (i + 1 < args.length) {
                    builder.model(args[++i].trim());
                }
            } else if (arg.startsWith("--model=")) {
                builder.model(arg.substring("--model=".length()).trim());
            } else if (arg.startsWith("-m=")) {
                builder.model(arg.substring("-m=".length()).trim());
            }
            // Instruction / Prompt direct (-i ou --instruction)
            else if (arg.equals("-i") || arg.equals("--instruction")) {
                if (i + 1 < args.length) {
                    builder.instruction(args[++i].trim());
                }
            } else if (arg.startsWith("--instruction=")) {
                builder.instruction(arg.substring("--instruction=".length()).trim());
            } else if (arg.startsWith("-i=")) {
                builder.instruction(arg.substring("-i=".length()).trim());
            }
            // Injection de code ou fichier source (-c ou --code)
            else if (arg.equals("-c") || arg.equals("--code")) {
                if (i + 1 < args.length) {
                    builder.code(resoudreContenuCode(args[++i].trim()));
                }
            } else if (arg.startsWith("--code=")) {
                builder.code(resoudreContenuCode(arg.substring("--code=".length()).trim()));
            } else if (arg.startsWith("-c=")) {
                builder.code(resoudreContenuCode(arg.substring("-c=".length()).trim()));
            }
            // Fichier source (-f ou --file)
            else if (arg.equals("-f") || arg.equals("--file")) {
                if (i + 1 < args.length) {
                    String chemin = args[++i].trim();
                    builder.filePath(chemin);
                    builder.instruction(resoudreContenuFichier(chemin));
                }
            } else if (arg.startsWith("--file=")) {
                String chemin = arg.substring("--file=".length()).trim();
                builder.filePath(chemin);
                builder.instruction(resoudreContenuFichier(chemin));
            } else if (arg.startsWith("-f=")) {
                String chemin = arg.substring("-f=".length()).trim();
                builder.filePath(chemin);
                builder.instruction(resoudreContenuFichier(chemin));
            }
            // Agent cible (-a ou --agent)
            else if (arg.equals("-a") || arg.equals("--agent")) {
                if (i + 1 < args.length) {
                    builder.agent(args[++i].trim());
                }
            } else if (arg.startsWith("--agent=")) {
                builder.agent(arg.substring("--agent=".length()).trim());
            } else if (arg.startsWith("-a=")) {
                builder.agent(arg.substring("-a=".length()).trim());
            }
            // Format de sortie / fichier (-o ou --output)
            else if (arg.equals("-o") || arg.equals("--output")) {
                if (i + 1 < args.length) {
                    builder.output(args[++i].trim());
                }
            } else if (arg.startsWith("--output=")) {
                builder.output(arg.substring("--output=".length()).trim());
            } else if (arg.startsWith("-o=")) {
                builder.output(arg.substring("-o=".length()).trim());
            }
            // Forçage de template (-t ou --template)
            else if (arg.equals("-t") || arg.equals("--template")) {
                if (i + 1 < args.length) {
                    builder.template(args[++i].trim());
                }
            } else if (arg.startsWith("--template=")) {
                builder.template(arg.substring("--template=".length()).trim());
            } else if (arg.startsWith("-t=")) {
                builder.template(arg.substring("-t=".length()).trim());
            }
            // Forçage de domaine (-d ou --domain)
            else if (arg.equals("-d") || arg.equals("--domain")) {
                if (i + 1 < args.length) {
                    builder.domain(args[++i].trim());
                }
            } else if (arg.startsWith("--domain=")) {
                builder.domain(arg.substring("--domain=".length()).trim());
            } else if (arg.startsWith("-d=")) {
                builder.domain(arg.substring("-d=".length()).trim());
            }
            // Langue cible (-l ou --language)
            else if (arg.equals("-l") || arg.equals("--language")) {
                if (i + 1 < args.length) {
                    builder.language(args[++i].trim());
                }
            } else if (arg.startsWith("--language=")) {
                builder.language(arg.substring("--language=".length()).trim());
            } else if (arg.startsWith("-l=")) {
                builder.language(arg.substring("-l=".length()).trim());
            }
            // Arguments positionnels (non préfixés par un tiret)
            else if (!arg.startsWith("-")) {
                positionalArgs.add(arg);
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
        } catch (Exception ignored) {
            // Pas un chemin de fichier valide, on traite comme un snippet inline
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
                System.err.println("Avertissement : Le fichier spécifié n'existe pas : " + chemin);
            }
        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier : " + e.getMessage());
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
