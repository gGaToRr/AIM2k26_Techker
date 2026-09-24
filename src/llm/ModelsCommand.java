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

    // Moteur llama.cpp qui execute les modeles. Couture de test : aucun test ne doit
    // atteindre le reseau (CONTRIBUTING.md).
    public interface Moteur {
        boolean present();
        boolean installer(PrintStream out, ModelInstaller.DownloadProgressListener listener);
    }

    private static final Moteur MOTEUR_LLAMA = new Moteur() {
        @Override public boolean present() { return LocalLlmBackend.detectRunnerBinary() != null; }
        @Override public boolean installer(PrintStream out, ModelInstaller.DownloadProgressListener listener) {
            return RuntimeInstaller.installer(java.nio.file.Path.of("llama"), out, listener);
        }
    };

    // Base de prompts (corpus/themes) : installee avec le premier modele, comme le moteur
    private static final Moteur BASE_PROMPTS = new Moteur() {
        @Override public boolean present() { return corpus.InstallationBase.installee(); }
        @Override public boolean installer(PrintStream out, ModelInstaller.DownloadProgressListener listener) {
            return corpus.InstallationBase.installer(out, listener);
        }
    };

    // Pour les tests qui ne portent pas sur la base : consideree comme presente
    private static final Moteur BASE_IGNOREE = new Moteur() {
        @Override public boolean present() { return true; }
        @Override public boolean installer(PrintStream out, ModelInstaller.DownloadProgressListener listener) { return true; }
    };

    private ModelsCommand() {}

    // Renvoie le code de sortie du processus
    public static int executer(CliArgs options, LlmConfig config, PrintStream out, Scanner scanner) {
        return executer(options, config, out, scanner, MOTEUR_LLAMA, BASE_PROMPTS);
    }

    public static int executer(CliArgs options, LlmConfig config, PrintStream out, Scanner scanner, Moteur moteur) {
        return executer(options, config, out, scanner, moteur, BASE_IGNOREE);
    }

    public static int executer(CliArgs options, LlmConfig config, PrintStream out, Scanner scanner, Moteur moteur,
                               Moteur base) {
        String repertoire = config != null ? config.getRepertoireModeles() : "models";
        // -o json : sortie structuree pour l'extension, qui fait sa propre mise en forme
        boolean json = options.hasOutput() && options.output().equalsIgnoreCase("json");

        if (options.isRuntimeInstall()) {
            boolean reussi = json
                    ? telechargerEnJson(out, (sortie, listener) -> moteur.installer(sortie, listener))
                    : moteur.installer(out, null);
            return reussi ? SUCCES : ECHEC;
        }

        if (options.isCorpusInstall()) {
            boolean reussi = json
                    ? telechargerEnJson(out, (sortie, listener) -> base.installer(sortie, listener))
                    : base.installer(out, null);
            return reussi ? SUCCES : ECHEC;
        }

        if (options.isModelsList() && json) {
            out.println(ModelManager.inventaireEnJson(repertoire, moteur.present(), base.present()));
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
            boolean moteurDisponible = moteur.present();
            if (options.hasOutput() && options.output().equalsIgnoreCase("json")) {
                out.println(ModelManager.verificationEnJson(ModelManager.verifier(repertoire, effective,
                        new LocalLlmBackend(), moteurDisponible), moteurDisponible));
            } else {
                ModelManager.verifierModeles(repertoire, effective, new LocalLlmBackend(), moteurDisponible, out);
            }
            return SUCCES;
        }

        if (options.hasModelsInstall()) {
            Optional<ModelType> modele = ModelType.fromAlias(options.modelsInstall());
            if (modele.isEmpty()) {
                return modeleInconnu(options.modelsInstall(), out);
            }
            // Un modele seul ne sert a rien : le moteur qui l'execute est installe avec lui
            // (un clone neuf n'a pas le dossier llama/, non versionne)
            ModelType choisi = modele.get();
            boolean reussi = json
                    ? telechargerEnJson(out, (sortie, listener) ->
                            ModelInstaller.telechargerModele(choisi, repertoire, sortie, listener)
                                    && installerMoteurSiAbsent(moteur, sortie, listener)
                                    && installerMoteurSiAbsent(base, sortie, listener))
                    : ModelInstaller.telechargerModele(choisi, repertoire, out, null)
                            && installerMoteurSiAbsent(moteur, out, null)
                            && installerMoteurSiAbsent(base, out, null);
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

    private static boolean installerMoteurSiAbsent(Moteur moteur, PrintStream out,
                                                   ModelInstaller.DownloadProgressListener listener) {
        return moteur.present() || moteur.installer(out, listener);
    }

    @FunctionalInterface
    private interface Telechargement {
        boolean executer(PrintStream sortie, ModelInstaller.DownloadProgressListener listener);
    }

    // Une ligne JSON par evenement : l'extension affiche la progression en direct.
    // Le texte habituel du telechargement est capture, pour en extraire la cause d'un echec.
    private static boolean telechargerEnJson(PrintStream out, Telechargement telechargement) {
        java.io.ByteArrayOutputStream journal = new java.io.ByteArrayOutputStream();
        PrintStream silencieux = new PrintStream(journal, true, java.nio.charset.StandardCharsets.UTF_8);

        boolean reussi = telechargement.executer(silencieux, (lus, total, vitesse) -> {
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
