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
        // -o json : sortie structuree pour l'extension, qui fait sa propre mise en forme
        boolean json = options.hasOutput() && options.output().equalsIgnoreCase("json");

        if (options.isModelsList() && json) {
            out.println(ModelManager.inventaireEnJson(repertoire));
            return SUCCES;
        }

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

        if (options.isModelsCheck()) {
            LlmConfig effective = config != null ? config : new LlmConfig();
            boolean moteur = LocalLlmBackend.detectRunnerBinary() != null;
            if (options.hasOutput() && options.output().equalsIgnoreCase("json")) {
                out.println(ModelManager.verificationEnJson(
                        ModelManager.verifier(repertoire, effective, new LocalLlmBackend(), moteur), moteur));
            } else {
                ModelManager.verifierModeles(repertoire, effective, new LocalLlmBackend(), moteur, out);
            }
            return SUCCES;
        }

        if (options.hasModelsInstall()) {
            Optional<ModelType> modele = ModelType.fromAlias(options.modelsInstall());
            if (modele.isEmpty()) {
                return modeleInconnu(options.modelsInstall(), out);
            }
            boolean reussi = json
                    ? telechargerEnJson(modele.get(), repertoire, out)
                    : ModelInstaller.telechargerModele(modele.get(), repertoire, out, null);
            if (!reussi) {
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
            // -y : la confirmation a deja ete donnee ailleurs (fenetre de l'extension)
            boolean accord = options.isYes() || ModelManager.confirmer(scanner, out,
                    "Supprimer " + modele.get().getNomAffiche() + " (" + etat.tailleLisible() + ") ?");
            if (!accord) {
                return SUCCES;
            }
            if (json) {
                java.io.ByteArrayOutputStream journal = new java.io.ByteArrayOutputStream();
                boolean supprime = ModelManager.supprimer(modele.get(), repertoire,
                        new PrintStream(journal, true, java.nio.charset.StandardCharsets.UTF_8));
                String message = journal.toString(java.nio.charset.StandardCharsets.UTF_8).trim()
                        .replaceFirst("^\\[.\\]\\s*", "");
                out.println("{\"supprime\":" + supprime + ",\"message\":" + util.Json.chaine(message) + "}");
                return supprime ? SUCCES : ECHEC;
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

    // Une ligne JSON par evenement : l'extension affiche la progression en direct.
    // Le texte habituel du telechargement est capture, pour en extraire la cause d'un echec.
    private static boolean telechargerEnJson(ModelType modele, String repertoire, PrintStream out) {
        java.io.ByteArrayOutputStream journal = new java.io.ByteArrayOutputStream();
        PrintStream silencieux = new PrintStream(journal, true, java.nio.charset.StandardCharsets.UTF_8);

        boolean reussi = ModelInstaller.telechargerModele(modele, repertoire, silencieux, (lus, total, vitesse) -> {
            out.println("{\"type\":\"progression\",\"lus\":" + lus + ",\"total\":" + total
                    + ",\"vitesse\":" + String.format(java.util.Locale.ROOT, "%.2f", vitesse) + "}");
            out.flush();
        });

        String message = "";
        if (!reussi) {
            message = journal.toString(java.nio.charset.StandardCharsets.UTF_8).lines()
                    .filter(l -> l.contains("[!]")).reduce((a, b) -> b)
                    .map(l -> l.replace("[!]", "").trim()).orElse("echec du telechargement");
        }
        out.println("{\"type\":\"fin\",\"ok\":" + reussi + ",\"message\":" + util.Json.chaine(message) + "}");
        out.flush();
        return reussi;
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
