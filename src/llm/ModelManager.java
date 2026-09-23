package llm;

import util.Log;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

// Cycle de vie des modeles installes : inventaire, fiche detaillee, suppression
// unitaire et purge (Issue #45).
//
// Le telechargement (Create) et la detection (Read) vivent dans ModelInstaller ;
// cette classe complete le cycle par Delete.
public final class ModelManager {

    // Etat d'un modele sur le disque
    public record ModelStatus(ModelType model, boolean installe, long tailleOctets, Optional<Instant> dateInstallation) {

        public String tailleLisible() {
            return installe ? formaterOctets(tailleOctets) : "-";
        }

        public String dateLisible() {
            return dateInstallation
                    .map(d -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .withZone(ZoneId.systemDefault()).format(d))
                    .orElse("-");
        }
    }

    private ModelManager() {}

    // --- Lecture ---

    public static List<ModelStatus> inventaire(String modelsDir) {
        List<ModelStatus> etats = new ArrayList<>();
        for (ModelType model : ModelType.getAllAvailable()) {
            etats.add(statutDe(model, modelsDir));
        }
        return etats;
    }

    public static ModelStatus statutDe(ModelType model, String modelsDir) {
        Path chemin = ModelInstaller.getModelPath(model, modelsDir);
        if (!Files.isRegularFile(chemin)) {
            return new ModelStatus(model, false, 0L, Optional.empty());
        }
        try {
            BasicFileAttributes attributs = Files.readAttributes(chemin, BasicFileAttributes.class);
            return new ModelStatus(model, true, attributs.size(),
                    Optional.of(attributs.creationTime().toInstant()));
        } catch (Exception e) {
            Log.exceptionIgnoree("Lecture des attributs de " + chemin, e);
            return new ModelStatus(model, true, 0L, Optional.empty());
        }
    }

    public static long espaceTotalOccupe(String modelsDir) {
        return inventaire(modelsDir).stream().filter(ModelStatus::installe)
                .mapToLong(ModelStatus::tailleOctets).sum();
    }

    // --- Suppression ---

    // Supprime le binaire d'un modele. Renvoie false si le modele n'etait pas installe.
    public static boolean supprimer(ModelType model, String modelsDir, PrintStream out) {
        Path chemin = ModelInstaller.getModelPath(model, modelsDir);
        if (!Files.isRegularFile(chemin)) {
            out.println("[*] " + model.getNomAffiche() + " n'est pas installe, rien a supprimer.");
            return false;
        }
        long taille = tailleSure(chemin);
        try {
            Files.delete(chemin);
            // Un telechargement interrompu laisse un .part a cote : le nettoyer aussi,
            // sinon l'espace n'est pas reellement libere.
            Files.deleteIfExists(Path.of(chemin + ".part"));
            out.println("[+] " + model.getNomAffiche() + " supprime (" + formaterOctets(taille) + " liberes).");
            return true;
        } catch (Exception e) {
            Log.erreur("Suppression de " + chemin, e);
            out.println("[!] Suppression impossible : " + e.getMessage());
            return false;
        }
    }

    // Supprime tous les modeles installes et reinitialise l'autorisation de telechargement
    public static int purger(String modelsDir, LlmConfig config, PrintStream out) {
        int supprimes = 0;
        for (ModelStatus etat : inventaire(modelsDir)) {
            if (etat.installe() && supprimer(etat.model(), modelsDir, out)) {
                supprimes++;
            }
        }

        if (config != null) {
            // La permission avait ete accordee pour un telechargement precis. Apres une
            // purge, la reconduire tacitement relancerait un telechargement de plusieurs
            // Go sans que l'utilisateur ne l'ait redemande.
            config.setPermissionAccordee(false);
            try {
                config.sauvegarderParDefaut();
            } catch (Exception e) {
                Log.exceptionIgnoree("Sauvegarde de la configuration apres purge", e);
            }
        }

        out.println("\n[+] Purge terminee : " + supprimes + " modele(s) supprime(s).");
        out.println("    L'autorisation de telechargement a ete reinitialisee.");
        return supprimes;
    }

    // --- Rendu texte ---

    public static String formaterInventaire(String modelsDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("*------------------------------------------*\n");
        sb.append("*        MODELES LOCAUX DISPONIBLES        *\n");
        sb.append("*------------------------------------------*\n\n");
        sb.append(String.format("%-20s %-32s %-12s %-10s %s%n",
                "ID", "NOM", "STATUT", "TAILLE", "INSTALLE LE"));
        sb.append("-".repeat(96)).append('\n');

        for (ModelStatus etat : inventaire(modelsDir)) {
            sb.append(String.format("%-20s %-32s %-12s %-10s %s%n",
                    etat.model().getId(),
                    etat.model().getNomAffiche(),
                    etat.installe() ? "installe" : "absent",
                    etat.tailleLisible(),
                    etat.dateLisible()));
        }

        sb.append("-".repeat(96)).append('\n');
        sb.append("Repertoire      : ").append(Path.of(repertoire(modelsDir)).toAbsolutePath()).append('\n');
        sb.append("Espace occupe   : ").append(formaterOctets(espaceTotalOccupe(modelsDir))).append('\n');
        return sb.toString();
    }

    public static String formaterFiche(ModelType model, String modelsDir) {
        ModelStatus etat = statutDe(model, modelsDir);
        StringBuilder sb = new StringBuilder();
        sb.append("*------------------------------------------*\n");
        sb.append("*             FICHE DU MODELE              *\n");
        sb.append("*------------------------------------------*\n\n");
        sb.append("   Identifiant    : ").append(model.getId()).append('\n');
        sb.append("   Nom            : ").append(model.getNomAffiche()).append('\n');
        sb.append("   Specialite     : ").append(model.getSpecialite()).append('\n');
        sb.append("   Description    : ").append(model.getDescription()).append('\n');
        sb.append("   Taille annoncee: ").append(model.getTailleDisque()).append('\n');
        sb.append("   RAM conseillee : ").append(model.getRamRecommandee()).append('\n');
        sb.append("   Archetypes     : ").append(model.getArchetypesCibles().isEmpty()
                ? "generaliste" : model.getArchetypesCibles()).append('\n');
        sb.append("   Source         : ").append(model.getUrlTelechargement()).append('\n');
        sb.append("   Empreinte      : ").append(model.getSha256()).append('\n');
        sb.append("\n   Statut local   : ").append(etat.installe() ? "installe" : "absent").append('\n');
        sb.append("   Chemin local   : ").append(ModelInstaller.getModelPath(model, modelsDir).toAbsolutePath()).append('\n');
        if (etat.installe()) {
            sb.append("   Taille reelle  : ").append(etat.tailleLisible()).append('\n');
            sb.append("   Installe le    : ").append(etat.dateLisible()).append('\n');
        }
        return sb.toString();
    }

    // --- Confirmation interactive ---

    // Toute suppression est irreversible et porte sur plus d'un Go : jamais sans accord.
    public static boolean confirmer(Scanner scanner, PrintStream out, String question) {
        out.print(question + " [o/N] : ");
        out.flush();
        String saisie = (scanner != null && scanner.hasNextLine()) ? scanner.nextLine().trim() : "n";
        boolean accepte = "o".equalsIgnoreCase(saisie) || "oui".equalsIgnoreCase(saisie)
                || "y".equalsIgnoreCase(saisie) || "yes".equalsIgnoreCase(saisie);
        if (!accepte) {
            out.println("[-] Operation annulee.");
        }
        return accepte;
    }

    // --- Utilitaires ---

    private static String repertoire(String modelsDir) {
        return (modelsDir != null && !modelsDir.isBlank()) ? modelsDir : "models";
    }

    private static long tailleSure(Path chemin) {
        try {
            return Files.size(chemin);
        } catch (Exception e) {
            Log.exceptionIgnoree("Taille de " + chemin, e);
            return 0L;
        }
    }

    public static String formaterOctets(long octets) {
        if (octets <= 0) return "0 o";
        if (octets < 1024) return octets + " o";
        double ko = octets / 1024.0;
        if (ko < 1024) return String.format("%.1f Ko", ko);
        double mo = ko / 1024.0;
        if (mo < 1024) return String.format("%.1f Mo", mo);
        return String.format("%.2f Go", mo / 1024.0);
    }
}
