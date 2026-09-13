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

// Gestionnaire d'onboarding pour débutants, de permissions et de téléchargement des modèles
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

    // Affiche le message explicatif complet et rassurant conçu pour les utilisateurs débutants
    public static void afficherMessageOnboardingDebutants(PrintStream out, ModelType recommendedModel) {
        out.println("╔════════════════════════════════════════════════════════════════════════════════╗");
        out.println("║         🌟 BIENVENUE DANS L'EXÉCUTION D'IA 100% LOCALE & PRIVÉE 🌟              ║");
        out.println("╚════════════════════════════════════════════════════════════════════════════════╝");
        out.println();
        out.println("💡 QU'EST-CE QUE C'EST ?");
        out.println("   Vous êtes sur le point d'activer un modèle d'IA (LLM léger) qui tournera");
        out.println("   DIRECTEMENT sur votre propre ordinateur, sans passer par Internet !");
        out.println();
        out.println("🛡️ VOS AVANTAGES CLÉS :");
        out.println("   1. 🔒 100% CONFIDENTIEL & HORS-LIGNE : Vos questions, vos codes et vos textes");
        out.println("      ne quittent JAMAIS votre machine. Aucune fuite de données vers un serveur tiers.");
        out.println("   2. 💸 100% GRATUIT & SANS ABONNEMENT : Pas de compte à créer, pas de carte bancaire,");
        out.println("      aucune clé API payante requise.");
        out.println("   3. ⚡ RAPIDE & AUTONOME : Fonctionne même sans connexion Internet une fois installé.");
        out.println();
        out.println("💾 ESPACE DISQUE REQUIS :");
        out.println("   Chaque modèle pèse entre 1.0 Go et 1.6 Go (fichiers hautement compressés).");
        out.println("   Le téléchargement s'effectue UNE SEULE FOIS dans le dossier local 'models/'.");
        out.println();
        out.println("🎯 MODÈLE RECOMMANDÉ POUR VOTRE REQUÊTE ACTUELLE :");
        if (recommendedModel != null) {
            out.println("   👉 " + recommendedModel.getNomAffiche());
            out.println("      • Spécialité : " + recommendedModel.getSpecialite());
            out.println("      • Description : " + recommendedModel.getDescription());
            out.println("      • Taille à télécharger : " + recommendedModel.getTailleDisque());
            out.println("      • RAM minimale recommandée : " + recommendedModel.getRamRecommandee());
        }
        out.println();
        out.println("──────────────────────────────────────────────────────────────────────────────────");
        out.println("Voulez-vous autoriser le téléchargement et l'activation du modèle local ?");
        out.println("   [1] OUI - Télécharger le modèle recommandé (" + (recommendedModel != null ? recommendedModel.getNomAffiche() : "Auto") + ") [Recommandé]");
        out.println("   [2] CHOISIR - Sélectionner un autre modèle dans la liste");
        out.println("   [3] NON / PLUS TARD - Générer uniquement le prompt sans exécuter de LLM local");
        out.println("──────────────────────────────────────────────────────────────────────────────────");
        out.print("👉 Votre choix (1, 2 ou 3, puis Entrée) : ");
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
            out.println("\n✅ Autorisation accordée ! Préparation du modèle " + recommendedModel.getNomAffiche() + "...\n");
            return recommendedModel;
        } else if ("2".equals(saisie)) {
            out.println("\n📋 Modèles légers disponibles :");
            List<ModelType> tous = ModelType.getAllAvailable();
            for (int i = 0; i < tous.size(); i++) {
                ModelType m = tous.get(i);
                out.printf("   [%d] %-30s | %-12s | %s%n", (i + 1), m.getNomAffiche(), m.getTailleDisque(), m.getSpecialite());
            }
            out.print("👉 Numéro du modèle souhaité (1-" + tous.size() + ") : ");
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
                    out.println("\n✅ Modèle sélectionné : " + choisi.getNomAffiche() + "\n");
                    return choisi;
                }
            } catch (NumberFormatException ignored) {}
            return recommendedModel;
        } else {
            out.println("\nℹ️ Téléchargement annulé. Génération du prompt optimisé en mode standard.\n");
            return null;
        }
    }

    // Télécharge un modèle avec affichage d'une barre de progression interactive
    public static boolean telechargerModele(ModelType model, String modelsDir, PrintStream out, DownloadProgressListener listener) {
        if (model == null) return false;
        Path targetFile = getModelPath(model, modelsDir);

        if (Files.exists(targetFile)) {
            out.println("✔ Le modèle " + model.getNomAffiche() + " est déjà installé (" + targetFile + ").");
            return true;
        }

        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }

            out.println("⬇️ Téléchargement de " + model.getNomAffiche() + " (" + model.getTailleDisque() + ")...");
            out.println("   Source officielle : " + model.getUrlTelechargement());
            out.println("   Destination locale : " + targetFile.toAbsolutePath());

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
                out.println("❌ Erreur de téléchargement HTTP (code " + response.statusCode() + ").");
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
            out.println("\n🎉 Téléchargement réussi et vérifié avec succès !");
            return true;

        } catch (Exception e) {
            out.println("\n❌ Échec du téléchargement : " + e.getMessage());
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
            out.printf("\r   Progression : [%s] %3d%% (%.1f / %.1f Mo à %.2f Mo/s)", bar, percent, readMB, totalMB, speedMBs);
        } else {
            out.printf("\r   Téléchargé : %.1f Mo à %.2f Mo/s...", readMB, speedMBs);
        }
        out.flush();
    }
}
