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

    public LlmBackend.GenerationResult execute(String metaPrompt, PromptProfile profile, String modelOverride, boolean interactive) {
        // 1. Routage intelligent vers le modèle expert
        ModelRouter.RoutingDecision routing = ModelRouter.resolve(modelOverride, profile);
        ModelType targetModel = routing.selectedModel();

        out.println("\n🧠 [Routage Intelligent] Modèle sélectionné : " + targetModel.getNomAffiche());
        out.println("   Justification : " + routing.rationale());
        out.println("   Confiance : " + (int)(routing.confidenceScore() * 100) + "%\n");

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
                out.println("⚠️ Le modèle " + targetModel.getNomAffiche() + " n'est pas encore installé localement.");
                out.println("   Lancez le programme en mode interactif pour télécharger le modèle avec l'assistant pour débutants.");
                return null;
            }
        }

        // Si le modèle n'est pas sur le disque mais la permission est acquise
        if (!isInstalled && config.isPermissionAccordee()) {
            boolean success = ModelInstaller.telechargerModele(targetModel, config.getRepertoireModeles(), out, null);
            if (!success) {
                out.println("❌ Impossible de télécharger le modèle. Poursuite sans exécution locale.");
                return null;
            }
        }

        // 3. Exécution de l'inférence locale
        out.println("⚡ [Inférence Locale en cours avec " + targetModel.getNomAffiche() + "] :");
        out.println("──────────────────────────────────────────────────────────────────────────────────");

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

            out.println("\n──────────────────────────────────────────────────────────────────────────────────");
            out.printf("📊 [Performance] %d tokens générés en %d ms (%.1f tokens/sec) via %s%n",
                    result.totalTokens(), result.elapsedMs(), result.tokensPerSecond(), targetModel.getNomAffiche());
            out.println();

            return result;

        } catch (Exception e) {
            out.println("\n❌ Erreur pendant l'inférence locale : " + e.getMessage());
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
