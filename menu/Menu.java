package menu;

import java.util.Scanner;

public class Menu {

    // Scanner pour lire l'entrée de l'utilisateur
    private final Scanner scanner;

    public Menu() {
        this.scanner = new Scanner(System.in);
    }

    // Message d'accueil du script
    public void afficherBienvenue() {
        System.out.println("=========================================");
        System.out.println("   Détecteur et Améliorateur de Prompt   ");
        System.out.println("=========================================\n");
    }

    // Méthode pour demander un prompt au user
    public String demanderPrompt() {
        System.out.print("Entrez votre prompt : ");
        return scanner.nextLine();
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
