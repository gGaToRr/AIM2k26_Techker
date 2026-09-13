package llm;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

// Gestionnaire d'installation et de téléchargement des modèles légers locaux
public class ModelInstaller {

    public interface DownloadProgressListener {
        void onProgress(long bytesRead, long totalBytes, double speedMBs);
    }

    // Vérifie si le fichier binaire du modèle est déjà présent sur le disque
    public static boolean isModelInstalled(ModelType model, String modelsDir) {
        if (model == null) return false;
        Path path = getModelPath(model, modelsDir);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public static Path getModelPath(ModelType model, String modelsDir) {
        String dir = (modelsDir != null && !modelsDir.isBlank()) ? modelsDir : "models";
        return Paths.get(dir, model.getNomFichier());
    }

    // Message sobre et clair aligné avec le style de Menu.java
    public static void afficherMessageOnboardingDebutants(PrintStream out, ModelType recommendedModel) {
        out.println("*------------------------------------------*");
        out.println("*  Prompting tool 4 a better work from AI  *");
        out.println("*------------------------------------------*");
        out.println("*      INSTALLATION DES MODELES LOCAUX     *");
        out.println("*------------------------------------------*\n");
        out.println("   Ce module optionnel permet d'executer vos");
        out.println("   prompts directement sur votre machine,");
        out.println("   sans aucune connexion Internet ni API.");
        out.println();
        out.println("   - 100% Hors-ligne & Prive (zero fuite de donnees)");
        out.println("   - 100% Gratuit (aucun abonnement ni cle API)");
        out.println("   - Espace disque requis : ~1.1 a 1.6 Go par modele");
        out.println("   - Le moteur d'inference natif (llama-cli) sera aussi installe");
        out.println("     automatiquement s'il n'est pas deja present sur la machine.");
        out.println();
        if (recommendedModel != null) {
            out.println("Modele recommande pour votre requete :");
            out.println("   * Nom        : " + recommendedModel.getNomAffiche());
            out.println("   * Specialite : " + recommendedModel.getSpecialite());
            out.println("   * Taille     : " + recommendedModel.getTailleDisque());
        }
        out.println("\n--------------------------------------------");
        out.println("Voulez-vous autoriser le telechargement du modele ?");
        out.println("   [1] Oui, telecharger le modele recommande (Recommande)");
        out.println("   [2] Choisir un autre modele dans la liste");
        out.println("   [3] Non, generer uniquement le prompt sans execution");
        out.println("--------------------------------------------");
        out.print("Votre choix (1, 2 ou 3) : ");
        out.flush();
    }

    // Demande interactivement la permission à l'utilisateur
    public static ModelType demanderPermissionUtilisateur(Scanner scanner, PrintStream out, ModelType recommendedModel, LlmConfig config) {
        afficherMessageOnboardingDebutants(out, recommendedModel);

        String saisie = scanner.hasNextLine() ? scanner.nextLine().trim() : "3";

        if ("1".equals(saisie) || "o".equalsIgnoreCase(saisie) || "oui".equalsIgnoreCase(saisie) || "y".equalsIgnoreCase(saisie)) {
            config.setPermissionAccordee(true);
            try {
                config.sauvegarderParDefaut();
            } catch (Exception ignored) {}
            out.println("\n[+] Autorisation accordee. Preparation de " + recommendedModel.getNomAffiche() + "...\n");
            return recommendedModel;
        } else if ("2".equals(saisie)) {
            out.println("\nModeles legers disponibles :");
            List<ModelType> tous = ModelType.getAllAvailable();
            for (int i = 0; i < tous.size(); i++) {
                ModelType m = tous.get(i);
                out.printf("   [%d] %-30s | %-12s | %s%n", (i + 1), m.getNomAffiche(), m.getTailleDisque(), m.getSpecialite());
            }
            out.print("Numero du modele souhaite (1-" + tous.size() + ") : ");
            out.flush();
            String numStr = scanner.hasNextLine() ? scanner.nextLine().trim() : "1";
            try {
                int num = Integer.parseInt(numStr);
                if (num >= 1 && num <= tous.size()) {
                    ModelType choisi = tous.get(num - 1);
                    config.setPermissionAccordee(true);
                    config.setModeleParDefaut(choisi.getId());
                    try {
                        config.sauvegarderParDefaut();
                    } catch (Exception ignored) {}
                    out.println("\n[+] Modele selectionne : " + choisi.getNomAffiche() + "\n");
                    return choisi;
                }
            } catch (NumberFormatException ignored) {}
            return recommendedModel;
        } else {
            out.println("\n[-] Telechargement ignore. Generation standard du prompt.\n");
            return null;
        }
    }

    // Télécharge un modèle avec affichage d'une barre de progression
    public static boolean telechargerModele(ModelType model, String modelsDir, PrintStream out, DownloadProgressListener listener) {
        if (model == null) return false;
        Path targetFile = getModelPath(model, modelsDir);

        if (Files.exists(targetFile)) {
            out.println("[*] Le modele " + model.getNomAffiche() + " est deja installe (" + targetFile + ").");
            return true;
        }

        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }

            out.println("[v] Telechargement de " + model.getNomAffiche() + " (" + model.getTailleDisque() + ")...");
            out.println("    Source      : " + model.getUrlTelechargement());
            out.println("    Destination : " + targetFile.toAbsolutePath());

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(model.getUrlTelechargement()))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                out.println("[!] Erreur de telechargement HTTP (code " + response.statusCode() + ").");
                return false;
            }

            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            Path tempFile = Paths.get(targetFile.toString() + ".part");

            try (InputStream in = new BufferedInputStream(response.body());
                 OutputStream fos = new BufferedOutputStream(Files.newOutputStream(tempFile))) {

                byte[] buffer = new byte[65536];
                int read;
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();
                long lastPrintTime = 0;

                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    totalBytes += read;

                    long now = System.currentTimeMillis();
                    if (now - lastPrintTime > 300) {
                        double elapsedSec = Math.max(0.001, (now - startTime) / 1000.0);
                        double speedMBs = (totalBytes / (1024.0 * 1024.0)) / elapsedSec;

                        if (listener != null) {
                            listener.onProgress(totalBytes, contentLength, speedMBs);
                        } else {
                            afficherBarreProgression(out, totalBytes, contentLength, speedMBs);
                        }
                        lastPrintTime = now;
                    }
                }
            }

            // Renommage atomique du fichier temporaire
            Files.move(tempFile, targetFile);
            out.println("\n[+] Telechargement termine avec succes.");
            return true;

        } catch (Exception e) {
            out.println("\n[!] Echec du telechargement : " + e.getMessage());
            return false;
        }
    }

    private static void afficherBarreProgression(PrintStream out, long bytesRead, long totalBytes, double speedMBs) {
        double readMB = bytesRead / (1024.0 * 1024.0);
        if (totalBytes > 0) {
            double totalMB = totalBytes / (1024.0 * 1024.0);
            int percent = (int) Math.min(100, (bytesRead * 100) / totalBytes);
            int barWidth = 30;
            int filled = (percent * barWidth) / 100;
            String bar = "=".repeat(Math.max(0, filled)) + (filled < barWidth ? ">" : "") + " ".repeat(Math.max(0, barWidth - filled - 1));
            out.printf("\r   Progression : [%s] %3d%% (%.1f / %.1f Mo a %.2f Mo/s)", bar, percent, readMB, totalMB, speedMBs);
        } else {
            out.printf("\r   Telecharge : %.1f Mo a %.2f Mo/s...", readMB, speedMBs);
        }
        out.flush();
    }
}
