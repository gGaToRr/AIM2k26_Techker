import cli.CliArgs;
import cli.CliClipboard;
import cli.CliParser;
import gen.MetaPromptEngine;
import llm.LlmConfig;
import llm.LlmEngine;
import llm.ModelInstaller;
import llm.ModelsCommand;
import menu.Menu;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.SafetyAdvisor;
import util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        // 0. Analyse des arguments de la ligne de commande
        CliArgs cliArgs = CliParser.parse(args);

        // Le niveau de diagnostic suit -V/--verbose. Le journal part sur stderr :
        // stdout reste reserve au produit, pour ne pas casser les usages en pipe.
        Log.configurerDepuisVerbose(cliArgs.isVerbose());

        // Flag --help / -h
        if (cliArgs.isHelp()) {
            System.out.println(CliParser.getHelpBanner());
            return;
        }

        // Flag --version / -v
        if (cliArgs.isVersion()) {
            System.out.println(CliParser.getVersionInfo());
            return;
        }

        // Commandes de gestion des modeles locaux : elles court-circuitent le pipeline
        // de generation, aucune instruction n'est requise.
        if (cliArgs.isCommandeModeles()) {
            System.exit(ModelsCommand.executer(cliArgs, LlmConfig.chargerParDefaut(),
                    System.out, new java.util.Scanner(System.in)));
        }

        // Récupération de l'instruction (via CLI ou mode interactif)
        String userPrompt;
        Menu menu = null;

        if (cliArgs.hasInstruction() || cliArgs.hasCode() || cliArgs.hasFilePath()) {
            userPrompt = cliArgs.getFullPrompt();
        } else {
            menu = new Menu();
            menu.afficherBienvenue();
            ModelInstaller.proposerInstallationAuDemarrage(menu.getScanner(), System.out, LlmConfig.chargerParDefaut());
            userPrompt = menu.demanderPrompt();
        }

        if (userPrompt == null || userPrompt.isBlank()) {
            if (menu != null) {
                menu.afficherResultat("Prompt vide.");
                menu.fermer();
            } else {
                System.err.println("Erreur : Aucun prompt ou instruction fourni.");
            }
            return;
        }

        // 1. Analyse préventive des termes sensibles (avertissement non bloquant)
        SafetyAdvisor.SafetyReport safety = SafetyAdvisor.analyser(userPrompt);
        if (safety.containsSensitiveTerms() && !cliArgs.isRaw()) {
            System.out.println("\n" + safety.warningMessage());
        }

        // 2. Analyse sémantique complète (NLP)
        PromptProfile profil = Lemmatizer.analyser(userPrompt);

        // Si le mode verbeux (-V) est activé, affichage du rapport détaillé NLP
        if (cliArgs.isVerbose() && !cliArgs.isRaw()) {
            System.out.println(CliParser.formatVerboseReport(profil));
        }

        // Mode dry-run (-n) : arrêt après l'analyse NLP
        if (cliArgs.isDryRun()) {
            if (!cliArgs.isRaw() && !cliArgs.isVerbose()) {
                System.out.println("\n--- [ANALYSE NLP DU PROMPT (-n / DRY-RUN)] ---");
                System.out.println("Nature détectée : " + profil.classification().primaryType());
                System.out.println("Confiance       : " + profil.classification().primaryProbability() + "% (" + profil.classification().confidenceLevel() + ")");
                System.out.println("Langue          : " + profil.language());
                if (!profil.detectedTechnologies().isEmpty()) {
                    System.out.println("Technologies    : " + profil.detectedTechnologies());
                }
                if (profil.domainInfo() != null && profil.domainInfo().isDomainIdentified()) {
                    System.out.println("Domaine & Sujet : " + profil.domainInfo().domainName() + " (" + profil.domainInfo().extractedTopic() + ")");
                }
                System.out.println("Tokens estimés  : " + profil.tokenMetrics().estimatedTokens());
                System.out.println("Score qualité   : " + profil.qualityDiagnostic().scoreGlobal() + "/100");
            }
            if (menu != null) menu.fermer();
            return;
        }

        // 3. Génération du prompt optimisé (Meta-Prompting avec JMustache et options)
        String promptOptimise = MetaPromptEngine.genererPromptOptimise(profil, cliArgs);

        // Détermination du contenu final selon le format demandé (-o json / md / txt)
        String contenuFinal = promptOptimise;
        boolean isJson = cliArgs.hasOutput() && (cliArgs.output().equalsIgnoreCase("json") || cliArgs.output().endsWith(".json"));
        if (isJson) {
            contenuFinal = MetaPromptEngine.genererExportJson(profil, promptOptimise, cliArgs);
        }

        // 4. Export vers fichier si un chemin a été fourni (-o output.md / -o output.json)
        if (cliArgs.hasOutput() && !cliArgs.output().equalsIgnoreCase("json") && !cliArgs.output().equalsIgnoreCase("md") && !cliArgs.output().equalsIgnoreCase("txt")) {
            try {
                Path outputPath = Path.of(cliArgs.output());
                Files.writeString(outputPath, contenuFinal);
                if (!cliArgs.isRaw()) {
                    System.out.println("\n[✓] Résultat exporté avec succès vers : " + outputPath.toAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("Erreur d'export vers le fichier " + cliArgs.output() + " : " + e.getMessage());
            }
        }

        // 5. Copie dans le presse-papiers si demandé (-C / --clipboard)
        if (cliArgs.isClipboard()) {
            boolean copieOk = CliClipboard.copierTexte(contenuFinal);
            if (!cliArgs.isRaw()) {
                if (copieOk) {
                    System.out.println("\n[✓] Prompt copié avec succès dans le presse-papiers.");
                } else {
                    System.err.println("\n[!] Avertissement : Impossible d'accéder au presse-papiers système.");
                }
            }
        }

        // 6. Affichage du prompt optimisé
        if (cliArgs.isRaw() || isJson) {
            // Mode brut pour pipeline Unix ou flux JSON
            System.out.println(contenuFinal);
        } else {
            if (!cliArgs.isVerbose()) {
                System.out.println("\n--- [1. ANALYSE DU PROMPT] ---");
                System.out.println("Nature détectée : " + profil.classification().primaryType());
                System.out.println("Confiance       : " + profil.classification().primaryProbability() + "% (" + profil.classification().confidenceLevel() + ")");
                System.out.println("Langue          : " + (cliArgs.hasLanguage() ? cliArgs.language() : profil.language()));
                if (!profil.detectedTechnologies().isEmpty()) {
                    System.out.println("Technologies    : " + profil.detectedTechnologies());
                }
                if (cliArgs.hasDomain()) {
                    System.out.println("Domaine Forcé   : " + cliArgs.domain());
                } else if (profil.domainInfo() != null && profil.domainInfo().isDomainIdentified()) {
                    System.out.println("Domaine & Sujet : " + profil.domainInfo().domainName() + " (" + profil.domainInfo().extractedTopic() + ")");
                }
                if (cliArgs.hasTemplate()) {
                    System.out.println("Template Forcé  : " + cliArgs.template());
                }
                if (cliArgs.hasAgent()) {
                    System.out.println("Agent Cible     : " + cliArgs.agent());
                }
                System.out.println("Tokens estimés  : " + profil.tokenMetrics().estimatedTokens());
                System.out.println("Score qualité   : " + profil.qualityDiagnostic().scoreGlobal() + "/100");
            }

            System.out.println("\n--- [2. PROMPT OPTIMISÉ POUR LE LLM] ---\n");
            System.out.println(contenuFinal);
            System.out.println("\n----------------------------------------");
        }

        // 7. Amélioration du prompt par le LLM local si demandée (-e / --exec), ou proposée en mode interactif
        boolean isInteractive = (menu != null);
        if (cliArgs.isExec() || (isInteractive && menu.proposerExecutionLocale())) {
            LlmEngine engine = isInteractive
                    ? new LlmEngine(null, LlmConfig.chargerParDefaut(), System.out, menu.getScanner())
                    : new LlmEngine();
            engine.execute(LlmEngine.construireDemandeAmelioration(profil), profil, cliArgs.model(), isInteractive);
        }

        // Fermeture du scanner si ouvert
        if (menu != null) {
            menu.fermer();
        }
    }
}
