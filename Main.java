import nlp.Lemmatizer;
import nlp.PromptProfile;

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

        // On lance l'analyse sémantique complète
        PromptProfile profil = Lemmatizer.analyser(userPrompt);

        // Affichage des données brutes
        System.out.println("\n--- Résultat de l'analyse ---");
        System.out.println("Nature détectée : " + profil.classification().primaryType());
        System.out.println("Confiance       : " + profil.classification().primaryProbability() + "% (" + profil.classification().confidenceLevel() + ")");
        System.out.println("Langue          : " + profil.language());
        System.out.println("Technologies    : " + profil.detectedTechnologies());
        System.out.println("Tokens estimés  : " + profil.tokenMetrics().estimatedTokens());
        System.out.println("Score qualité   : " + profil.qualityDiagnostic().scoreGlobal() + "/100");

        // Fermeture du scanner
        menu.fermer();
    }
}
