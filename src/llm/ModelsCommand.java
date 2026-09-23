package llm;

import cli.CliArgs;
import util.Log;

import java.io.PrintStream;
import java.util.Optional;
import java.util.Scanner;

// Point d'entree des commandes de gestion des modeles locaux (Issue #45).
//
// Isole du pipeline de generation : ces commandes ne produisent pas de prompt,
// elles inspectent ou modifient l'etat du disque.
public final class ModelsCommand {

    public static final int SUCCES = 0;
    public static final int ECHEC = 1;

    private ModelsCommand() {}

    // Renvoie le code de sortie du processus
    public static int executer(CliArgs options, LlmConfig config, PrintStream out, Scanner scanner) {
        String repertoire = config != null ? config.getRepertoireModeles() : "models";

        if (options.isModelsList()) {
            out.print(ModelManager.formaterInventaire(repertoire));
            return SUCCES;
        }

        if (options.hasModelsInfo()) {
            Optional<ModelType> modele = ModelType.fromAlias(options.modelsInfo());
            if (modele.isEmpty()) {
                return modeleInconnu(options.modelsInfo(), out);
            }
            out.print(ModelManager.formaterFiche(modele.get(), repertoire));
            return SUCCES;
        }

        if (options.hasModelsInstall()) {
            Optional<ModelType> modele = ModelType.fromAlias(options.modelsInstall());
            if (modele.isEmpty()) {
                return modeleInconnu(options.modelsInstall(), out);
            }
            if (!ModelInstaller.telechargerModele(modele.get(), repertoire, out, null)) {
                return ECHEC;
            }
            // Installer explicitement un modele vaut accord pour l'execution locale
            if (config != null && !config.isPermissionAccordee()) {
                config.setPermissionAccordee(true);
                try {
                    config.sauvegarderParDefaut();
                } catch (Exception e) {
                    Log.exceptionIgnoree("Sauvegarde de la configuration LLM apres installation", e);
                }
            }
            return SUCCES;
        }

        if (options.hasModelsDelete()) {
            Optional<ModelType> modele = ModelType.fromAlias(options.modelsDelete());
            if (modele.isEmpty()) {
                return modeleInconnu(options.modelsDelete(), out);
            }
            ModelManager.ModelStatus etat = ModelManager.statutDe(modele.get(), repertoire);
            if (!etat.installe()) {
                out.println("[*] " + modele.get().getNomAffiche() + " n'est pas installe, rien a supprimer.");
                return SUCCES;
            }
            boolean accord = ModelManager.confirmer(scanner, out,
                    "Supprimer " + modele.get().getNomAffiche() + " (" + etat.tailleLisible() + ") ?");
            if (!accord) {
                return SUCCES;
            }
            return ModelManager.supprimer(modele.get(), repertoire, out) ? SUCCES : ECHEC;
        }

        if (options.isModelsPurge()) {
            long occupe = ModelManager.espaceTotalOccupe(repertoire);
            if (occupe == 0) {
                out.println("[*] Aucun modele installe, rien a purger.");
                return SUCCES;
            }
            boolean accord = ModelManager.confirmer(scanner, out,
                    "Supprimer TOUS les modeles installes (" + ModelManager.formaterOctets(occupe) + ") ?");
            if (!accord) {
                return SUCCES;
            }
            ModelManager.purger(repertoire, config, out);
            return SUCCES;
        }

        return SUCCES;
    }

    private static int modeleInconnu(String saisie, PrintStream out) {
        out.println("[!] Modele inconnu : " + saisie);
        out.println("    Modeles disponibles :");
        for (ModelType m : ModelType.getAllAvailable()) {
            out.println("      - " + m.getId());
        }
        return ECHEC;
    }
}
