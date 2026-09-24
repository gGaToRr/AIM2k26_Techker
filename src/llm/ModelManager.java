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

    // --- Verification (bouton "Tester mes modeles installes" de l'extension) ---

    // Plafond volontairement bas : il s'agit de prouver que le modele repond, pas de produire un texte
    static final int TOKENS_TEST_GENERATION = 16;

    public enum EtatFichier { ABSENT, INTACT, ENDOMMAGE, ILLISIBLE }

    public enum EtatGeneration { OK, ECHEC, NON_TESTEE }

    // Resultat de la verification d'un modele du registre, installe ou non
    public record Verification(ModelStatus statut, EtatFichier fichier, String detailFichier,
                               EtatGeneration generation, String detailGeneration, double secondes) {

        public boolean fonctionnel() {
            return fichier == EtatFichier.INTACT && generation == EtatGeneration.OK;
        }

        Verification avecGeneration(EtatGeneration etat, String detail, double duree) {
            return new Verification(statut, fichier, detailFichier, etat, detail, duree);
        }

        Verification avecGenerationTestee(Essai essai) {
            return avecGeneration(essai.etat(), essai.detail(), essai.secondes());
        }
    }

    // Verifie tous les modeles du registre : pour ceux installes, integrite du fichier
    // (taille puis SHA-256), puis une generation tres courte si le moteur est disponible.
    public static List<Verification> verifier(String modelsDir, LlmConfig config, LlmBackend backend,
                                              boolean moteurDisponible) {
        List<Verification> resultats = new ArrayList<>();
        for (ModelStatus etat : inventaire(modelsDir)) {
            if (!etat.installe()) {
                resultats.add(new Verification(etat, EtatFichier.ABSENT, "", EtatGeneration.NON_TESTEE, "", 0));
                continue;
            }
            ModelType model = etat.model();
            Verification fichier = verifierFichier(etat, ModelInstaller.getModelPath(model, modelsDir));

            if (fichier.fichier() != EtatFichier.INTACT) {
                resultats.add(fichier.avecGeneration(EtatGeneration.NON_TESTEE, "fichier endommage", 0));
            } else if (!moteurDisponible) {
                resultats.add(fichier.avecGeneration(EtatGeneration.NON_TESTEE, "moteur llama.cpp absent", 0));
            } else {
                resultats.add(fichier.avecGenerationTestee(tester(model, modelsDir, config, backend)));
            }
        }
        return resultats;
    }

    // Rendu texte pour le terminal. Renvoie le nombre de modeles pleinement fonctionnels.
    public static int verifierModeles(String modelsDir, LlmConfig config, LlmBackend backend,
                                      boolean moteurDisponible, PrintStream out) {
        List<Verification> resultats = verifier(modelsDir, config, backend, moteurDisponible);
        List<Verification> installes = resultats.stream().filter(v -> v.fichier() != EtatFichier.ABSENT).toList();
        out.println("Modeles installes : " + installes.size() + " / " + resultats.size());

        if (installes.isEmpty()) {
            out.println("\nAucun modele installe. Utilisez \"Telecharger les modeles\".");
            return 0;
        }
        if (!moteurDisponible) {
            out.println("[!] Moteur llama.cpp introuvable : la generation ne sera pas testee.");
        }

        for (Verification v : installes) {
            out.println();
            out.println((v.fonctionnel() ? "[OK] " : v.fichier() == EtatFichier.INTACT ? "[?] " : "[X] ")
                    + v.statut().model().getNomAffiche());
            out.println("    Fichier    : " + v.fichier().name().toLowerCase()
                    + " (" + v.statut().tailleLisible() + ", " + v.detailFichier() + ")");
            out.println("    Generation : " + switch (v.generation()) {
                case OK -> String.format("OK en %.1f s", v.secondes());
                case ECHEC -> "echec (" + v.detailGeneration() + ")";
                case NON_TESTEE -> "non testee (" + v.detailGeneration() + ")";
            });
        }

        long fonctionnels = installes.stream().filter(Verification::fonctionnel).count();
        out.println();
        out.println("Bilan : " + fonctionnels + " modele(s) fonctionnel(s) sur " + installes.size() + " installe(s).");
        return (int) fonctionnels;
    }

    // Inventaire pour la page de telechargement de l'extension
    // moteurPresent : le moteur llama.cpp qui execute les modeles est-il installe ?
    public static String inventaireEnJson(String modelsDir, boolean moteurPresent) {
        StringBuilder sb = new StringBuilder("{\"moteur\":{\"installe\":").append(moteurPresent)
                .append(",\"version\":").append(util.Json.chaine(RuntimeInstaller.VERSION))
                .append("},\"modeles\":[");
        List<ModelStatus> etats = inventaire(modelsDir);
        for (int i = 0; i < etats.size(); i++) {
            ModelStatus etat = etats.get(i);
            ModelType model = etat.model();
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(util.Json.chaine(model.getId()))
              .append(",\"nom\":").append(util.Json.chaine(model.getNomAffiche()))
              .append(",\"specialite\":").append(util.Json.chaine(model.getSpecialite()))
              .append(",\"tailleDisque\":").append(util.Json.chaine(model.getTailleDisque()))
              .append(",\"installe\":").append(etat.installe())
              .append(",\"tailleOctets\":").append(etat.tailleOctets())
              .append('}');
        }
        return sb.append("]}").toString();
    }

    // Rendu JSON pour l'extension, qui se charge de la mise en forme
    public static String verificationEnJson(List<Verification> resultats, boolean moteurDisponible) {
        StringBuilder sb = new StringBuilder("{\"moteurDisponible\":").append(moteurDisponible).append(",\"modeles\":[");
        for (int i = 0; i < resultats.size(); i++) {
            Verification v = resultats.get(i);
            ModelType model = v.statut().model();
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(util.Json.chaine(model.getId()))
              .append(",\"nom\":").append(util.Json.chaine(model.getNomAffiche()))
              .append(",\"tailleOctets\":").append(v.statut().tailleOctets())
              .append(",\"fichier\":").append(util.Json.chaine(v.fichier().name()))
              .append(",\"detailFichier\":").append(util.Json.chaine(v.detailFichier()))
              .append(",\"generation\":").append(util.Json.chaine(v.generation().name()))
              .append(",\"detailGeneration\":").append(util.Json.chaine(v.detailGeneration()))
              .append(",\"secondes\":").append(String.format(java.util.Locale.ROOT, "%.2f", v.secondes()))
              .append('}');
        }
        return sb.append("]}").toString();
    }

    // Taille d'abord (instantane), puis empreinte SHA-256 (quelques secondes par Go)
    private static Verification verifierFichier(ModelStatus etat, Path chemin) {
        ModelType model = etat.model();
        if (model.getTailleOctets() > 0 && etat.tailleOctets() != model.getTailleOctets()) {
            return new Verification(etat, EtatFichier.ENDOMMAGE, "taille " + etat.tailleLisible()
                    + ", attendu " + formaterOctets(model.getTailleOctets()), EtatGeneration.NON_TESTEE, "", 0);
        }
        if (model.getSha256() == null || model.getSha256().isBlank()) {
            return new Verification(etat, EtatFichier.INTACT, "pas d'empreinte de reference", EtatGeneration.NON_TESTEE, "", 0);
        }
        try {
            boolean conforme = model.getSha256().equalsIgnoreCase(ModelInstaller.calculerSha256(chemin));
            return new Verification(etat, conforme ? EtatFichier.INTACT : EtatFichier.ENDOMMAGE,
                    conforme ? "SHA-256 conforme" : "SHA-256 different de la reference", EtatGeneration.NON_TESTEE, "", 0);
        } catch (Exception e) {
            return new Verification(etat, EtatFichier.ILLISIBLE, e.getMessage(), EtatGeneration.NON_TESTEE, "", 0);
        }
    }

    private record Essai(EtatGeneration etat, String detail, double secondes) {}

    private static Essai tester(ModelType model, String modelsDir, LlmConfig config, LlmBackend backend) {
        long debut = System.currentTimeMillis();
        try {
            LlmConfig configTest = new LlmConfig(config.isPermissionAccordee(), config.getModeleParDefaut(),
                    modelsDir, 0.0, TOKENS_TEST_GENERATION, false);
            LlmBackend.GenerationResult resultat = backend.generate(model, "Bonjour", configTest, null);
            double secondes = (System.currentTimeMillis() - debut) / 1000.0;
            boolean ok = resultat.fullText() != null && !resultat.fullText().isBlank();
            return new Essai(ok ? EtatGeneration.OK : EtatGeneration.ECHEC, ok ? "" : "reponse vide", secondes);
        } catch (Exception e) {
            return new Essai(EtatGeneration.ECHEC, e.getMessage(), (System.currentTimeMillis() - debut) / 1000.0);
        }
    }

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
