package llm;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;
import util.Ecran;
import util.Log;

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

    public static void afficherMessageOnboardingDebutants(PrintStream out, ModelType recommendedModel) {
        afficherMessageOnboardingDebutants(out, recommendedModel, false);
    }

    // Au demarrage, aucune requete n'a encore ete saisie : le texte s'adapte
    public static void afficherMessageOnboardingDebutants(PrintStream out, ModelType recommendedModel, boolean auDemarrage) {
        Ecran.etape(out, "INSTALLATION DES MODELES LOCAUX (optionnel)");
        out.println("   Ce module optionnel permet d'executer vos");
        out.println("   prompts directement sur votre machine,");
        out.println("   sans aucune connexion Internet ni API.");
        out.println();
        out.println("   - 100% Hors-ligne & Prive (zero fuite de donnees)");
        out.println("   - 100% Gratuit (aucun abonnement ni cle API)");
        out.println("   - Espace disque requis : ~1.1 a 1.6 Go par modele");
        out.println();
        if (recommendedModel != null) {
            out.println(auDemarrage ? "Modele recommande :" : "Modele recommande pour votre requete :");
            out.println("   * Nom        : " + recommendedModel.getNomAffiche());
            out.println("   * Specialite : " + recommendedModel.getSpecialite());
            out.println("   * Taille     : " + recommendedModel.getTailleDisque());
        }
        out.println("\n--------------------------------------------");
        out.println("Voulez-vous autoriser le telechargement du modele ?");
        out.println("   [1] Oui, telecharger le modele recommande (Recommande)");
        out.println("   [2] Choisir un autre modele dans la liste");
        out.println(auDemarrage
                ? "   [3] Non, passer directement au prompt"
                : "   [3] Non, generer uniquement le prompt sans execution");
        out.println("--------------------------------------------");
        out.print("Votre choix (1, 2 ou 3) : ");
        out.flush();
    }

    // Au lancement du menu : tant qu'aucun modele n'est sur le disque, on propose l'installation.
    // Un refus n'est pas memorise : la question revient au prochain lancement.
    public static void proposerInstallationAuDemarrage(Scanner scanner, PrintStream out, LlmConfig config) {
        String repertoire = config.getRepertoireModeles();
        boolean aucunModele = ModelManager.inventaire(repertoire).stream()
                .noneMatch(ModelManager.ModelStatus::installe);
        if (!aucunModele) {
            return;
        }
        ModelType recommande = ModelType.fromAlias(config.getModeleParDefaut()).orElse(ModelType.QWEN_CODER);
        ModelType choisi = demanderPermissionUtilisateur(scanner, out, recommande, config, true);
        if (choisi != null) {
            telechargerModele(choisi, repertoire, out, null);
        }
    }

    // Demande interactivement la permission à l'utilisateur
    public static ModelType demanderPermissionUtilisateur(Scanner scanner, PrintStream out, ModelType recommendedModel, LlmConfig config) {
        return demanderPermissionUtilisateur(scanner, out, recommendedModel, config, false);
    }

    public static ModelType demanderPermissionUtilisateur(Scanner scanner, PrintStream out, ModelType recommendedModel,
                                                          LlmConfig config, boolean auDemarrage) {
        afficherMessageOnboardingDebutants(out, recommendedModel, auDemarrage);

        String saisie = scanner.hasNextLine() ? scanner.nextLine().trim() : "3";

        if ("1".equals(saisie) || "o".equalsIgnoreCase(saisie) || "oui".equalsIgnoreCase(saisie) || "y".equalsIgnoreCase(saisie)) {
            config.setPermissionAccordee(true);
            try {
                config.sauvegarderParDefaut();
            } catch (Exception e) {
                Log.exceptionIgnoree("Sauvegarde de la configuration LLM apres accord de permission", e);
            }
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
                    } catch (Exception e) {
                        Log.exceptionIgnoree("Sauvegarde de la configuration LLM apres choix du modele", e);
                    }
                    out.println("\n[+] Modele selectionne : " + choisi.getNomAffiche() + "\n");
                    return choisi;
                }
            } catch (NumberFormatException e) {
                Log.exceptionIgnoree("Numero de modele non numerique, retour au modele recommande", e);
            }
            return recommendedModel;
        } else {
            out.println("\n[-] Telechargement ignore.");
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

            // Verification d'integrite avant de considerer le fichier comme installe :
            // un binaire corrompu ou substitue ne doit jamais atteindre llama-cli.
            if (!verifierIntegrite(model, tempFile, out)) {
                supprimerSilencieusement(tempFile);
                return false;
            }

            // Renommage atomique du fichier temporaire
            Files.move(tempFile, targetFile);
            out.println("\n[+] Telechargement termine avec succes.");
            return true;

        } catch (Exception e) {
            out.println("\n[!] Echec du telechargement : " + e.getMessage());
            supprimerSilencieusement(Paths.get(targetFile.toString() + ".part"));
            return false;
        }
    }

    // Compare la taille puis l'empreinte SHA-256 du fichier telecharge a la reference du registre
    public static boolean verifierIntegrite(ModelType model, Path fichier, PrintStream out) {
        if (model.getSha256() == null || model.getSha256().isBlank()) {
            out.println("\n[!] Aucune empreinte de reference connue pour " + model.getNomAffiche() + ", verification ignoree.");
            return true;
        }
        return verifierIntegrite(fichier, model.getSha256(), model.getTailleOctets(), out);
    }

    // Surcharge sur valeurs explicites : permet de couvrir chaque branche en TDD
    // sans manipuler un fichier de plusieurs Go.
    public static boolean verifierIntegrite(Path fichier, String attendu, long tailleAttendue, PrintStream out) {
        try {
            long tailleReelle = Files.size(fichier);
            if (tailleAttendue > 0 && tailleReelle != tailleAttendue) {
                out.println("\n[!] Taille inattendue : " + tailleReelle + " octets recus, " + tailleAttendue + " attendus.");
                out.println("    Le fichier est incomplet ou ne correspond pas au modele annonce.");
                return false;
            }

            out.println("\n[v] Verification de l'integrite (SHA-256)...");
            String obtenu = calculerSha256(fichier);

            if (!attendu.equalsIgnoreCase(obtenu)) {
                out.println("[!] EMPREINTE INVALIDE : le fichier telecharge ne correspond pas a la reference.");
                out.println("    Attendu : " + attendu);
                out.println("    Obtenu  : " + obtenu);
                out.println("    Le fichier a ete supprime. Relancez le telechargement.");
                return false;
            }

            out.println("[+] Integrite confirmee.");
            return true;

        } catch (Exception e) {
            out.println("\n[!] Verification d'integrite impossible : " + e.getMessage());
            return false;
        }
    }

    // Calcule l'empreinte SHA-256 d'un fichier en flux, sans le charger en memoire
    public static String calculerSha256(Path fichier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(Files.newInputStream(fichier))) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest.digest()) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static void supprimerSilencieusement(Path fichier) {
        try {
            Files.deleteIfExists(fichier);
        } catch (Exception ignored) {}
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
