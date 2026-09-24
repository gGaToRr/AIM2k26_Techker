package llm;

import nlp.PromptProfile;
import java.io.PrintStream;
import java.util.Scanner;

// Orchestrateur de haut niveau pour l'exécution des LLMs locaux avec routage et onboarding
public class LlmEngine {

    private final LlmBackend backend;
    private final LlmConfig config;
    private final PrintStream out;
    private final Scanner in;

    public LlmEngine() {
        this(new LocalLlmBackend(), LlmConfig.chargerParDefaut(), System.out, new Scanner(System.in));
    }

    public LlmEngine(LlmBackend backend, LlmConfig config, PrintStream out, Scanner in) {
        this.backend = backend != null ? backend : new LocalLlmBackend();
        this.config = config != null ? config : new LlmConfig();
        this.out = out != null ? out : System.out;
        this.in = in != null ? in : new Scanner(System.in);
    }

    // Message envoyé au modèle local : le prompt brut et les indices de l'analyse NLP.
    // Le méta-prompt complet est trop long pour un modèle de 1.5B.
    public static String construireDemandeAmelioration(PromptProfile profil) {
        return construireDemandeAmelioration(profil, null);
    }

    // langueCible : langue imposee au prompt ameliore (null = celle du prompt brut)
    public static String construireDemandeAmelioration(PromptProfile profil, String langueCible) {
        StringBuilder sb = new StringBuilder();
        sb.append("Prompt brut a ameliorer :\n").append(profil.rawText().trim()).append("\n\n");
        sb.append("Indications de l'analyse automatique :\n");
        sb.append("- Type de demande : ").append(profil.classification().primaryType()).append('\n');
        sb.append("- Langue : ").append(profil.language()).append('\n');
        if (!profil.detectedTechnologies().isEmpty()) {
            sb.append("- Technologies : ").append(String.join(", ", profil.detectedTechnologies())).append('\n');
        }
        if (profil.domainInfo() != null && profil.domainInfo().isDomainIdentified()) {
            sb.append("- Domaine : ").append(profil.domainInfo().domainName()).append('\n');
        }
        // Rappel en fin de message : un petit modele suit surtout la derniere consigne lue
        sb.append("\nRAPPEL : ne realise PAS la tache. Aucun code, aucune solution, aucune explication. "
                + "Ecris uniquement le prompt ameliore, puis arrete-toi.");
        // La consigne systeme demande la langue du prompt brut : la derniere consigne l'emporte
        if (langueCible != null && !langueCible.isBlank()) {
            sb.append(" Redige tout le prompt ameliore en ").append(gen.MetaPromptEngine.libelleLangue(langueCible))
                    .append(", meme si le prompt brut est dans une autre langue.");
        }
        return sb.toString();
    }

    public LlmBackend.GenerationResult execute(String metaPrompt, PromptProfile profile, String modelOverride, boolean interactive) {
        // 1. Routage intelligent vers le modèle expert
        ModelRouter.RoutingDecision routing = ModelRouter.resolve(modelOverride, profile);
        ModelType targetModel = routing.selectedModel();

        out.println("\n--- [ROUTAGE DU MODELE EXPERT] ---");
        out.println("Modele selectionne : " + targetModel.getNomAffiche());
        out.println("Justification       : " + routing.rationale());
        out.println("Confiance           : " + (int)(routing.confidenceScore() * 100) + "%\n");

        // 2. Vérification de l'installation et onboarding si premier lancement
        boolean isInstalled = ModelInstaller.isModelInstalled(targetModel, config.getRepertoireModeles());

        // Sans modèle forcé, un modèle déjà installé vaut mieux qu'un téléchargement
        // silencieux de plus d'1 Go pour le spécialiste du routage.
        boolean modeleForce = modelOverride != null && !modelOverride.isBlank() && !"auto".equalsIgnoreCase(modelOverride);
        if (!isInstalled && !modeleForce) {
            for (ModelType candidat : ModelType.getAllAvailable()) {
                if (ModelInstaller.isModelInstalled(candidat, config.getRepertoireModeles())) {
                    out.println("[*] " + targetModel.getNomAffiche() + " n'est pas installe : utilisation de "
                            + candidat.getNomAffiche() + ".");
                    targetModel = candidat;
                    isInstalled = true;
                    break;
                }
            }
        }

        if (!isInstalled && !config.isPermissionAccordee()) {
            if (interactive) {
                ModelType accepted = ModelInstaller.demanderPermissionUtilisateur(in, out, targetModel, config);
                if (accepted == null) {
                    return null;
                }
                targetModel = accepted;
                isInstalled = ModelInstaller.isModelInstalled(targetModel, config.getRepertoireModeles());
            } else {
                out.println("[!] Avertissement : Le modele " + targetModel.getNomAffiche() + " n'est pas encore installe localement.");
                out.println("    Installez-le avec : --models-install " + targetModel.getId() + "  (ou lancez l'outil sans argument)");
                return null;
            }
        }

        // Si le modèle n'est pas sur le disque mais la permission est acquise
        if (!isInstalled && config.isPermissionAccordee()) {
            boolean success = ModelInstaller.telechargerModele(targetModel, config.getRepertoireModeles(), out, null);
            if (!success) {
                out.println("[!] Impossible de telecharger le modele. Poursuite sans execution locale.");
                return null;
            }
        }

        // 3. Exécution de l'inférence locale
        out.println("\n--- [PROMPT AMELIORE PAR LE MODELE LOCAL (" + targetModel.getNomAffiche() + ")] ---\n");

        try {
            LlmBackend.GenerationResult result = backend.generate(targetModel, metaPrompt, config, token -> {
                if (config.isStreamingActive()) {
                    out.print(token);
                    out.flush();
                }
            });

            if (!config.isStreamingActive()) {
                out.println(result.fullText());
            }

            out.println("\n\n--------------------------------------------");
            out.printf("Statistiques : %d tokens generes en %d ms (%.1f tokens/sec)%n",
                    result.totalTokens(), result.elapsedMs(), result.tokensPerSecond());
            out.println("--------------------------------------------\n");

            return result;

        } catch (Exception e) {
            out.println("\n[!] Erreur pendant l'inference locale : " + e.getMessage());
            return null;
        }
    }

    public LlmConfig getConfig() {
        return config;
    }

    public LlmBackend getBackend() {
        return backend;
    }
}
