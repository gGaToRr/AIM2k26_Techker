import gen.MetaPromptEngine;
import menu.Menu;
import nlp.Lemmatizer;
import nlp.PromptProfile;
import nlp.SafetyAdvisor;

public class Main {
    public static void main(String[] args) {
        // Initialisation du menu
        Menu menu = new Menu();

        // Affichage de l'accueil et saisie du prompt
        menu.afficherBienvenue();
        String userPrompt = menu.demanderPrompt();

        if (userPrompt == null || userPrompt.isBlank()) {
            menu.afficherResultat("Prompt vide.");
            menu.fermer();
            return;
        }

        // 1. Analyse préventive des termes sensibles (avertissement non bloquant)
        SafetyAdvisor.SafetyReport safety = SafetyAdvisor.analyser(userPrompt);
        if (safety.containsSensitiveTerms()) {
            System.out.println("\n" + safety.warningMessage());
        }

        // 2. Analyse sémantique complète (NLP)
        PromptProfile profil = Lemmatizer.analyser(userPrompt);

        // 3. Génération du prompt optimisé (Meta-Prompting avec JMustache)
        String promptOptimise = MetaPromptEngine.genererPromptOptimise(profil);

        // 4. Affichage du résultat
        System.out.println("\n--- [1. ANALYSE DU PROMPT] ---");
        System.out.println("Nature détectée : " + profil.classification().primaryType());
        System.out.println("Confiance       : " + profil.classification().primaryProbability() + "% (" + profil.classification().confidenceLevel() + ")");
        System.out.println("Langue          : " + profil.language());
        System.out.println("Technologies    : " + profil.detectedTechnologies());
        System.out.println("Tokens estimés  : " + profil.tokenMetrics().estimatedTokens());
        System.out.println("Score qualité   : " + profil.qualityDiagnostic().scoreGlobal() + "/100");

        System.out.println("\n--- [2. PROMPT OPTIMISÉ POUR LE LLM] ---\n");
        System.out.println(promptOptimise);
        System.out.println("\n----------------------------------------");

        // Fermeture du scanner
        menu.fermer();
    }
}
