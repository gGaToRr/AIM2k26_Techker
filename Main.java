import cli.CliArgs;
import cli.CliParser;
import gen.MetaPromptEngine;
import menu.Menu;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.SafetyAdvisor;

public class Main {
    public static void main(String[] args) {
        // 0. Analyse des arguments de la ligne de commande
        CliArgs cliArgs = CliParser.parse(args);

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

        // Récupération de l'instruction (via CLI ou mode interactif)
        String userPrompt;
        Menu menu = null;

        if (cliArgs.hasInstruction() || cliArgs.hasCode() || cliArgs.hasFilePath()) {
            userPrompt = cliArgs.getFullPrompt();
        } else {
            menu = new Menu();
            menu.afficherBienvenue();
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
        if (safety.containsSensitiveTerms()) {
            System.out.println("\n" + safety.warningMessage());
        }

        // 2. Analyse sémantique complète (NLP)
        PromptProfile profil = Lemmatizer.analyser(userPrompt);

        // Si le mode verbeux (-V) est activé, affichage du rapport détaillé NLP
        if (cliArgs.isVerbose()) {
            System.out.println(CliParser.formatVerboseReport(profil));
        }

        // Mode dry-run (-n) : arrêt après l'analyse NLP
        if (cliArgs.isDryRun()) {
            if (menu != null) menu.fermer();
            return;
        }

        // 3. Génération du prompt optimisé (Meta-Prompting avec JMustache)
        String promptOptimise = MetaPromptEngine.genererPromptOptimise(profil);

        // 4. Affichage du résultat
        if (cliArgs.isRaw()) {
            // Mode brut pour pipeline Unix
            System.out.println(promptOptimise);
        } else {
            if (!cliArgs.isVerbose()) {
                System.out.println("\n--- [1. ANALYSE DU PROMPT] ---");
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

            System.out.println("\n--- [2. PROMPT OPTIMISÉ POUR LE LLM] ---\n");
            System.out.println(promptOptimise);
            System.out.println("\n----------------------------------------");
        }

        // Fermeture du scanner si ouvert
        if (menu != null) {
            menu.fermer();
        }
    }
}
