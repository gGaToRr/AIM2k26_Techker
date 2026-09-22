package llm;

import nlp.PromptProfile;
import java.io.PrintStream;
import java.util.Scanner;

// Orchestrateur de haut niveau pour l'exécution des LLMs locaux avec routage et onboarding
public class LlmEngine {

    // Couture d'installation du runtime natif : injectable pour permettre aux tests
    // de couvrir le provisionnement sans declencher de telechargement reseau.
    public interface RuntimeProvisioner {
        boolean installer(String runtimeDir, PrintStream out);
    }

    private final LlmBackend backend;
    private final LlmConfig config;
    private final PrintStream out;
    private final Scanner in;
    private final RuntimeProvisioner runtimeProvisioner;

    public LlmEngine() {
        this(new LocalLlmBackend(), LlmConfig.chargerParDefaut(), System.out, new Scanner(System.in));
    }

    public LlmEngine(LlmBackend backend, LlmConfig config, PrintStream out, Scanner in) {
        this(backend, config, out, in, RuntimeInstaller::telechargerEtInstallerRuntime);
    }

    public LlmEngine(LlmBackend backend, LlmConfig config, PrintStream out, Scanner in,
                     RuntimeProvisioner runtimeProvisioner) {
        this.backend = backend != null ? backend : new LocalLlmBackend();
        this.config = config != null ? config : new LlmConfig();
        this.out = out != null ? out : System.out;
        this.in = in != null ? in : new Scanner(System.in);
        this.runtimeProvisioner = runtimeProvisioner != null
                ? runtimeProvisioner
                : RuntimeInstaller::telechargerEtInstallerRuntime;
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
                out.println("    Lancez l'outil sans argument pour le telecharger via l'assistant.");
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

        // 2.bis Le runtime natif conditionne l'inference reelle : il doit etre verifie a chaque
        // execution et pas seulement quand le modele vient d'etre telecharge. Un .gguf deja
        // present sur le disque sans llama-cli faisait sinon retomber le backend sur une
        // reponse simulee, en silence (Issue #55).
        assurerRuntimeNatif(interactive);

        // 3. Exécution de l'inférence locale
        out.println("\n--- [REPONSE DU MODELE LOCAL (" + targetModel.getNomAffiche() + ")] ---\n");

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

    // Garantit la presence du moteur d'inference natif avant toute generation.
    // Le mode degrade reste possible mais n'est jamais silencieux.
    private void assurerRuntimeNatif(boolean interactive) {
        if (LocalLlmBackend.isRuntimeAvailable(config.getRepertoireRuntime())) {
            return;
        }

        if (!config.isPermissionAccordee()) {
            if (!interactive || !ModelInstaller.demanderPermissionRuntime(in, out, config)) {
                out.println("[!] Moteur d'inference natif (llama-cli) absent et telechargement non autorise.");
                out.println("    Execution en mode degrade : la reponse affichee sera simulee, pas generee par le modele.");
                return;
            }
        }

        boolean runtimeOk = runtimeProvisioner.installer(config.getRepertoireRuntime(), out);
        if (!runtimeOk) {
            out.println("[!] Runtime natif indisponible. Execution en mode degrade (reponse simulee) en attendant.");
        }
    }

    public LlmConfig getConfig() {
        return config;
    }

    public LlmBackend getBackend() {
        return backend;
    }
}
