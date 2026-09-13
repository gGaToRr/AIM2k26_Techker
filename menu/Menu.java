package menu;

import java.io.IOException;
import java.util.Scanner;

public class Menu {

    // Scanner pour lire l'entrée de l'utilisateur
    private final Scanner scanner;

    public Menu() {
        this(new Scanner(System.in));
    }

    public Menu(Scanner scanner) {
        this.scanner = scanner;
    }

    public Menu(java.io.InputStream inputStream) {
        this(new Scanner(inputStream));
    }

    // Message d'accueil du script
    public void afficherBienvenue() {
        System.out.println("*------------------------------------------*");
        System.out.println("*  Prompting tool 4 a better work from AI  *");
        System.out.println("*------------------------------------------*\n");
        System.out.println("   This tool was build for everyone, he is  ");
        System.out.println("   light and powerfull. All you need to do  ");
        System.out.println("   to send your prompt in the tool and      ");
        System.out.println("   drop the output in your model (All       ");
        System.out.println("   models are working). - Thanks 4 using    \n");
        System.out.println("          Pierre Untersinger - **\n");
        System.out.println("\n--------------------------------------------\n");
    }

    // Méthode pour demander un prompt au user (support multiligne, copier-coller et redirection de flux)
    public String demanderPrompt() {
        System.out.println("Entrez votre prompt (collez votre texte/code, puis validez avec ':done' ou une ligne vide) :");

        StringBuilder promptBuilder = new StringBuilder();
        int consecutiveEmptyLines = 0;

        while (scanner.hasNextLine()) {
            String ligne = scanner.nextLine();

            // Commande de validation explicite
            if (ligne.trim().equalsIgnoreCase(":done") || ligne.trim().equalsIgnoreCase(":end")) {
                break;
            }

            if (ligne.trim().isEmpty()) {
                consecutiveEmptyLines++;
                try {
                    int available = System.in.available();
                    if (available == 0 && (promptBuilder.length() > 0 || consecutiveEmptyLines >= 1)) {
                        break;
                    }
                } catch (IOException ignored) {
                    if (consecutiveEmptyLines >= 2 || promptBuilder.length() > 0) {
                        break;
                    }
                }
            } else {
                consecutiveEmptyLines = 0;
            }

            promptBuilder.append(ligne).append("\n");
        }

        return promptBuilder.toString().trim();
    }

    // Affiche un résultat ou un message
    public void afficherResultat(String message) {
        System.out.println("\n" + message);
    }

    // Ferme proprement le scanner
    public void fermer() {
        scanner.close();
    }
}
